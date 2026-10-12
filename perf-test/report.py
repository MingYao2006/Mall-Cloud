# -*- coding: utf-8 -*-
import json, os
PERF = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(PERF, "results.json"), encoding="utf-8"))
order = [("C_cache_hit","命中详情缓存"), ("B_null_cache","命中空值缓存"), ("A_penetrate","缓存穿透(无防护)")]
rows = []
for k, name in order:
    s = d[k]
    rows.append((k, name, s))
hdr = f"| {'分组':<16} | {'QPS':>8} | {'Avg(ms)':>8} | {'P95(ms)':>8} | {'P99(ms)':>8} | {'DB查询':>8} | {'DB/请求':>8} |"
print(hdr); print("|" + "-"*18 + "|" + "-"*10 + "|" + "-"*10 + "|" + "-"*10 + "|" + "-"*10 + "|" + "-"*10 + "|" + "-"*10 + "|")
for k, name, s in rows:
    print(f"| {name:<14} | {s['qps']:>8} | {s['avg']:>8} | {s['p95']:>8} | {s['p99']:>8} | {s['db_selects']:>8} | {s['selects_per_req']:>8} |")
print()
A, B, C = d["A_penetrate"], d["B_null_cache"], d["C_cache_hit"]
def pct(new, old): return (new-old)/old*100
print("== A(穿透) vs B(空值缓存) —— 同为查询不存在的商品 ==")
print(f"  DB 查询: {A['db_selects']} -> {B['db_selects']}   降低 {abs(pct(B['db_selects'],A['db_selects'])):.1f}%")
print(f"  QPS    : {A['qps']} -> {B['qps']}   提升 {pct(B['qps'],A['qps']):.1f}%")
print(f"  Avg RT : {A['avg']}ms -> {B['avg']}ms   降低 {abs(pct(B['avg'],A['avg'])):.1f}%")
print(f"  P95    : {A['p95']}ms -> {B['p95']}ms   降低 {abs(pct(B['p95'],A['p95'])):.1f}%")
print()
print("== A(穿透) vs C(命中详情缓存) ==")
print(f"  DB 查询: {A['db_selects']} -> {C['db_selects']}   降低 {abs(pct(C['db_selects'],A['db_selects'])):.1f}%")
print(f"  QPS    : {A['qps']} -> {C['qps']}   提升 {pct(C['qps'],A['qps']):.1f}%")
print(f"  Avg RT : {A['avg']}ms -> {C['avg']}ms   降低 {abs(pct(C['avg'],A['avg'])):.1f}%")
print(f"  P99    : {A['p99']}ms -> {C['p99']}ms   降低 {abs(pct(C['p99'],A['p99'])):.1f}%")
print()
for k,name in order:
    s=d[k]; print(f"{name}: 样本={s['requests']} 期间={s['duration_s']}s 响应码={s['codes']}")
