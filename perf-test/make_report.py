# -*- coding: utf-8 -*-
import json, os
PERF = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(PERF, "results.json"), encoding="utf-8"))
A, B, C = d["A_penetrate"], d["B_null_cache"], d["C_cache_hit"]
def pct(new, old): return (new - old) / old * 100

def bar(label, value, maxv, unit, color):
    w = max(2, value / maxv * 100)
    return (f'<div class="row"><span class="lbl">{label}</span>'
            f'<span class="barwrap"><span class="bar" style="width:{w:.1f}%;background:{color}"></span></span>'
            f'<span class="val">{value}{unit}</span></div>')

html = f"""<!DOCTYPE html><html lang="zh"><head><meta charset="utf-8">
<title>Mall 商品详情接口压测对照报告</title>
<style>
 body{{font-family:"Microsoft YaHei",system-ui,sans-serif;background:#f5f6f8;margin:0;padding:32px;color:#1f2328}}
 .card{{background:#fff;border-radius:12px;padding:28px 32px;max-width:1080px;margin:0 auto 24px;box-shadow:0 1px 3px rgba(0,0,0,.08)}}
 h1{{font-size:22px;margin:0 0 6px}} h2{{font-size:16px;margin:28px 0 14px;padding-left:10px;border-left:4px solid #2f6feb}}
 .sub{{color:#656d76;font-size:13px;margin-bottom:8px}}
 table{{border-collapse:collapse;width:100%;font-size:14px}}
 th,td{{border-bottom:1px solid #e6e8eb;padding:9px 10px;text-align:right}}
 th:first-child,td:first-child{{text-align:left}}
 thead th{{background:#f0f2f5;font-weight:600;border-bottom:2px solid #d8dbe0}}
 tr.best td{{background:#eaf7ee;font-weight:600}}
 tr.worst td{{background:#fdeeee}}
 .row{{display:flex;align-items:center;gap:10px;margin:7px 0;font-size:13px}}
 .lbl{{width:190px;color:#424a53;flex:none}}
 .barwrap{{flex:1;background:#eef0f3;border-radius:5px;height:20px;overflow:hidden}}
 .bar{{display:block;height:100%;border-radius:5px}}
 .val{{width:96px;text-align:right;font-variant-numeric:tabular-nums;font-weight:600;color:#1f2328}}
 .gain{{display:flex;gap:14px;flex-wrap:wrap;margin-top:6px}}
 .kpi{{flex:1;min-width:190px;background:#f7f9fc;border:1px solid #e3e8ee;border-radius:10px;padding:14px 16px}}
 .kpi .n{{font-size:26px;font-weight:700;color:#1a7f37}}
 .kpi .t{{font-size:12px;color:#656d76;margin-top:4px}}
 .note{{font-size:12.5px;color:#656d76;line-height:1.9;margin-top:10px}}
 code{{background:#f0f2f5;padding:1px 5px;border-radius:4px;font-size:12.5px}}
</style></head><body>

<div class="card">
 <h1>Mall-Cloud 商品详情读接口 · 高并发压测对照报告</h1>
 <div class="sub">压测工具 JMeter 5.6.3 ｜ 入口 <code>GET http://localhost:9000/api/c/product/{{id}}</code>（网关 → C 端聚合 → 商品服务）</div>
 <div class="sub">每组 100 并发线程 · 5s 爬坡 · 100 循环 = <b>10,000 请求</b>；DB 压力用 MySQL <code>Com_select</code> 增量统计</div>

 <h2>三组结果</h2>
 <table>
  <thead><tr><th>分组</th><th>QPS</th><th>Avg (ms)</th><th>P95 (ms)</th><th>P99 (ms)</th><th>DB 查询次数</th><th>DB 查询/请求</th></tr></thead>
  <tbody>
   <tr class="best"><td>C · 命中详情缓存（存在的商品，已预热）</td><td>{C['qps']}</td><td>{C['avg']}</td><td>{C['p95']}</td><td>{C['p99']}</td><td>{C['db_selects']}</td><td>{C['selects_per_req']}</td></tr>
   <tr><td>B · 命中空值缓存（固定 30 个不存在的 ID）</td><td>{B['qps']}</td><td>{B['avg']}</td><td>{B['p95']}</td><td>{B['p99']}</td><td>{B['db_selects']}</td><td>{B['selects_per_req']}</td></tr>
   <tr class="worst"><td>A · 缓存穿透（每次全新不存在 ID＝无防护）</td><td>{A['qps']}</td><td>{A['avg']}</td><td>{A['p95']}</td><td>{A['p99']}</td><td>{A['db_selects']}</td><td>{A['selects_per_req']}</td></tr>
  </tbody>
 </table>

 <h2>QPS 对比（越高越好）</h2>
 {bar('C 命中详情缓存', C['qps'], 1000, ' req/s', '#1a7f37')}
 {bar('B 命中空值缓存', B['qps'], 1000, ' req/s', '#bf8700')}
 {bar('A 缓存穿透（无防护）', A['qps'], 1000, ' req/s', '#cf222e')}

 <h2>平均响应时间对比（越低越好）</h2>
 {bar('C 命中详情缓存', C['avg'], 200, ' ms', '#1a7f37')}
 {bar('B 命中空值缓存', B['avg'], 200, ' ms', '#bf8700')}
 {bar('A 缓存穿透（无防护）', A['avg'], 200, ' ms', '#cf222e')}

 <h2>DB 查询次数对比（越低越好，共 10,000 请求）</h2>
 {bar('C 命中详情缓存', C['db_selects'], 10000, ' 次', '#1a7f37')}
 {bar('B 命中空值缓存', B['db_selects'], 10000, ' 次', '#bf8700')}
 {bar('A 缓存穿透（无防护）', A['db_selects'], 10000, ' 次', '#cf222e')}

 <h2>A → B：空值缓存防穿透带来的提升</h2>
 <div class="sub">两组请求语义完全相同（都查"不存在的商品"），唯一差别是 ID 是否重复、空值缓存能否命中。</div>
 <div class="gain">
  <div class="kpi"><div class="n">-{abs(pct(B['db_selects'], A['db_selects'])):.1f}%</div><div class="t">DB 查询 {A['db_selects']} → {B['db_selects']} 次</div></div>
  <div class="kpi"><div class="n">+{pct(B['qps'], A['qps']):.1f}%</div><div class="t">QPS {A['qps']} → {B['qps']}</div></div>
  <div class="kpi"><div class="n">-{abs(pct(B['avg'], A['avg'])):.1f}%</div><div class="t">平均响应 {A['avg']} → {B['avg']} ms</div></div>
  <div class="kpi"><div class="n">-{abs(pct(B['p95'], A['p95'])):.1f}%</div><div class="t">P95 {A['p95']} → {B['p95']} ms</div></div>
 </div>

 <h2>A → C：缓存命中带来的提升</h2>
 <div class="gain">
  <div class="kpi"><div class="n">-{abs(pct(C['db_selects'], A['db_selects'])):.1f}%</div><div class="t">DB 查询 {A['db_selects']} → {C['db_selects']} 次</div></div>
  <div class="kpi"><div class="n">+{pct(C['qps'], A['qps']):.1f}%</div><div class="t">QPS {A['qps']} → {C['qps']}</div></div>
  <div class="kpi"><div class="n">-{abs(pct(C['avg'], A['avg'])):.1f}%</div><div class="t">平均响应 {A['avg']} → {C['avg']} ms</div></div>
  <div class="kpi"><div class="n">-{abs(pct(C['p99'], A['p99'])):.1f}%</div><div class="t">P99 {A['p99']} → {C['p99']} ms</div></div>
 </div>

 <h2>口径说明（面试可追问）</h2>
 <div class="note">
  · <b>A 组不是"把缓存关掉"</b>，而是用 700 万个取值空间的随机 ID 让每个请求都是全新的 key，因此空值缓存和详情缓存<b>必然全部 miss</b>，效果等价于"没有缓存防护"。<br>
  · <b>B 组与 A 组请求语义一致</b>（都是查询不存在的商品），差别只在 ID 是否复用，所以两组可直接对比。<br>
  · C 组是正常业务路径（查存在的商品，缓存已预热）。<br>
  · A/B 组响应码为 400（商品不存在，业务预期响应，非系统错误）；三组均<b>无 5xx、无超时</b>。<br>
  · 单机压测，压测机与被压服务同机，QPS 绝对值仅用于横向对比，不代表生产容量。
 </div>
</div>
</body></html>"""
open(os.path.join(PERF, "压测报告.html"), "w", encoding="utf-8").write(html)
print("OK ->", os.path.join(PERF, "压测报告.html"))
