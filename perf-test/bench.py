# -*- coding: utf-8 -*-
"""Mall 商品详情读接口压测驱动：A/B 对照 + DB 压力采样 + JTL 解析。"""
from __future__ import annotations
import argparse, json, os, subprocess, sys, time
from collections import Counter

import redis

PERF = os.path.dirname(os.path.abspath(__file__))
JMETER = r"D:\Jmeter\apache-jmeter-5.6.3\apache-jmeter-5.6.3\bin\jmeter.bat"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
JMX = os.path.join(PERF, "product-detail.jmx")
REDIS_URL = "redis://localhost:6379/0"

# 分组定义：id_min, id_max 决定请求的商品 ID 分布
GROUPS = {
    "C_cache_hit":  dict(id_min=1,       id_max=30,      warmup=True,  clear_null=False,
                         desc="命中 Redis 详情缓存（存在的商品，已预热）"),
    "B_null_cache": dict(id_min=900001,  id_max=900030,  warmup=False, clear_null=True,
                         desc="命中空值缓存（固定 30 个不存在的 ID 循环）"),
    "A_penetrate":  dict(id_min=2000000, id_max=9000000, warmup=False, clear_null=True,
                         desc="缓存穿透（每次都是全新的不存在 ID，等价于无防护）"),
}


def mysql_scalar(sql: str) -> str:
    r = subprocess.run([MYSQL, "-uroot", "-p123456", "-N", "-B", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    return (r.stdout or "").strip()


def com_select() -> int:
    s = mysql_scalar("SHOW GLOBAL STATUS LIKE 'Com_select';")
    try:
        return int(s.split("\t")[1])
    except Exception:
        print("读取 Com_select 失败:", s, file=sys.stderr)
        return -1


def prepare(group: str, r: redis.Redis) -> None:
    g = GROUPS[group]
    if g["clear_null"]:
        n = 0
        for k in r.scan_iter(match="product:null:*", count=500):
            r.delete(k); n += 1
        print(f"  清理空值缓存 key: {n}")
    if g["warmup"]:
        import urllib.request
        for pid in range(g["id_min"], g["id_max"] + 1):
            try:
                urllib.request.urlopen(f"http://localhost:9000/api/c/product/{pid}", timeout=10).read()
            except Exception:
                pass
        print(f"  预热 {g['id_min']}~{g['id_max']} 完成，当前 product:* key = "
              f"{sum(1 for _ in r.scan_iter(match='product:*', count=500))}")


def run_jmeter(tag, threads, ramp, loops, id_min, id_max) -> tuple[str, str]:
    jtl = os.path.join(PERF, f"result_{tag}.jtl")
    log = os.path.join(PERF, f"jmeter_{tag}.log")
    for f in (jtl, log):
        if os.path.exists(f):
            os.remove(f)
    cmd = (f'"{JMETER}" -n -t "{JMX}" -l "{jtl}" -j "{log}" '
           f"-JTHREADS={threads} -JRAMP={ramp} -JLOOPS={loops} "
           f"-JID_MIN={id_min} -JID_MAX={id_max} -JHOST=localhost -JPORT=9000")
    subprocess.run(cmd, shell=True, cwd=PERF, stdout=subprocess.DEVNULL,
                   stderr=subprocess.DEVNULL)
    return jtl, log


def parse_jtl(path: str) -> dict:
    rows = []
    with open(path, encoding="utf-8", errors="replace") as f:
        header = f.readline().rstrip("\n").split(",")
        idx = {name: i for i, name in enumerate(header)}
        for line in f:
            p = line.rstrip("\n").split(",")
            if len(p) < len(header):
                continue
            rows.append(p)
    if not rows:
        return {}
    n = len(rows)
    elapsed = sorted(int(r[idx["elapsed"]]) for r in rows)
    codes = Counter(r[idx["responseCode"]] for r in rows)
    ts = [int(r[idx["timeStamp"]]) for r in rows]
    end = [int(r[idx["timeStamp"]]) + int(r[idx["elapsed"]]) for r in rows]

    def pct(p):
        if not elapsed:
            return 0
        i = min(len(elapsed) - 1, int(round((p / 100.0) * len(elapsed))) - 1)
        return elapsed[max(i, 0)]

    duration = (max(end) - min(ts)) / 1000.0
    return dict(
        requests=n,
        duration_s=round(duration, 2),
        qps=round(n / duration, 1) if duration > 0 else 0,
        avg=round(sum(elapsed) / n, 1),
        p50=pct(50), p90=pct(90), p95=pct(95), p99=pct(99),
        max=elapsed[-1], min=elapsed[0],
        codes=dict(codes),
    )


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--group", required=True, choices=list(GROUPS))
    ap.add_argument("--threads", type=int, default=50)
    ap.add_argument("--ramp", type=int, default=5)
    ap.add_argument("--loops", type=int, default=60)
    a = ap.parse_args()

    g = GROUPS[a.group]
    r = redis.from_url(REDIS_URL, decode_responses=True)
    print(f"=== 分组 [{a.group}] {g['desc']} ===")
    print(f"    线程={a.threads} 爬坡={a.ramp}s 循环={a.loops} "
          f"总请求={a.threads * a.loops} ID范围={g['id_min']}~{g['id_max']}")
    prepare(a.group, r)
    time.sleep(1)
    db_before = com_select()
    print(f"    Com_select 起始 = {db_before}")
    t0 = time.time()
    jtl, _ = run_jmeter(a.group, a.threads, a.ramp, a.loops, g["id_min"], g["id_max"])
    wall = time.time() - t0
    db_after = com_select()
    stats = parse_jtl(jtl)
    if not stats:
        print("    !! 结果为空，压测可能失败"); return

    stats.update(group=a.group, desc=g["desc"], threads=a.threads, loops=a.loops,
                 db_before=db_before, db_after=db_after,
                 db_selects=db_after - db_before, wall_s=round(wall, 1),
                 selects_per_req=round((db_after - db_before) / stats["requests"], 3))
    print(f"    Com_select 结束 = {db_after}   增量 = {db_after - db_before}")
    print(f"    请求={stats['requests']}  耗时={stats['duration_s']}s  QPS={stats['qps']}")
    print(f"    Avg={stats['avg']}ms  P50={stats['p50']}  P90={stats['p90']}  "
          f"P95={stats['p95']}  P99={stats['p99']}  Max={stats['max']}")
    print(f"    响应码={stats['codes']}  DB查询={stats['db_selects']} "
          f"({stats['selects_per_req']}/请求)")
    out = os.path.join(PERF, "results.json")
    allres = json.load(open(out, encoding="utf-8")) if os.path.exists(out) else {}
    allres[a.group] = stats
    json.dump(allres, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"    已写入 {out}")


if __name__ == "__main__":
    main()
