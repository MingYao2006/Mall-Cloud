# 商品列表页索引优化：实测结果

环境：MySQL 8.0.46（本地），`product_bench` 20 万行 / 20 分类 / 93% 上架，`detail` 全为 NULL（与线上一致）。
方法：`CREATE TABLE product_bench LIKE product` 复制真实表结构与原索引 → 造确定性数据（不用 `RAND()`，保证前后两轮数据完全相同）→ `EXPLAIN ANALYZE` 逐条对比。每条查询先空跑一次热缓冲池，取第二次。

## 主对比

| # | 查询形状 | 优化前 | 优化后 | 提升 |
|---|---|---|---|---|
| Q1 | 分类+上架，按价格倒序取 20 | **113 ms**，index merge intersect 扫 `idx_status` **187,987** 条索引项 + **filesort** 1 万行 | **0.088 ms**，走 `idx_list_status_cat_price` 倒序，**只读 20 行**，无 filesort | **~1280×** |
| Q2 | 分类+上架，按 sort 升序取 20 | 113 ms，同上（filesort） | 0.118 ms，走 `idx_list_status_cat_sort`，读 20 行 | ~960× |
| Q3 | 分类+价格区间 500~3000，价格升序 | 0.76 ms，`idx_price` range，读 320 行后过滤 | 0.087 ms，复合索引直接定位区间，读 20 行 | ~8.7× |
| Q4 | 不传分类，全站按价格倒序 | 0.07 ms，`idx_price` 倒序提前终止 | 0.13 ms，**优化器仍选 `idx_price`，新索引没被采用** | 无收益 |
| Q5 | 全站价格倒序，`OFFSET 100000` | 657 ms，`idx_status` + filesort 10 万行 | 705 ms，**执行计划没变** | 无收益 |

新增索引：

```sql
ALTER TABLE product ADD INDEX idx_list_status_cat_price (is_deleted, status, category_id, price);
ALTER TABLE product ADD INDEX idx_list_status_cat_sort  (is_deleted, status, category_id, sort);
ALTER TABLE product ADD INDEX idx_list_status_price     (is_deleted, status, price);
```

## 四个反直觉结论（面试就讲这些）

**1. 优化前的计划是 index merge intersect，这是典型的坏计划。**
`idx_category_id` 和 `idx_status` 两条单列索引被求交集，为了过滤 93% 上架的商品，扫了 **187,987** 条 `idx_status` 索引项，最后还要 filesort 1 万行。低基数列（status 只有 0/1/2）的单列索引不但没用，还诱导优化器做了个比全表扫描更贵的决定。**这是"单列索引堆一堆"这个新手误区的实证**，比讲道理有说服力。

**2. Q4 证伪了我自己的设计。**
我原本认为"不传分类、全站按价格排"必须配一条 `(is_deleted, status, price)`，实测优化器**继续用老的 `idx_price` 倒序扫描**并在 20 行提前终止，因为 93% 的行都是上架的，往前扫 20 条几乎必然满足条件。**所以这条索引对 Q4 是冗余的，本来该 DROP。**
但实验 C 里它又活了过来：延迟关联的子查询把它当**覆盖索引**用（`Covering index lookup`），10 万条 id 完全不回表。
→ 结论：**索引有没有用不是看单条 SQL，而是看它服务于哪条查询路径。** 判断冗余必须把所有查询路径一起看，只看一条会误删。

**3. 实验 A：列顺序错了，性能差 10 倍。**
建一条"错顺序"的 `(is_deleted, status, price, category_id)` 并 `FORCE INDEX`：排序列 price 跑到 category_id 前面，导致 category_id 退化成回表前的逐行过滤，**读了 370 行才凑够 20 条**（正确顺序读 20 行），耗时 0.879 ms vs 0.088 ms。这就是"等值列必须全部排在排序列之前"的实证，不是背出来的。

**4. 实验 B：低基数列 status 放不放进索引，这个分布下几乎没差别。**
`(is_deleted, category_id, price)`（不含 status）跑出 0.081 ms / 20 行，和四列索引基本一样。**所以不要说"低基数列必须放索引最左"** —— 真正的理由是：status 是每条查询必带的等值条件，放进索引前缀能让后面的列保持有序、并在下架商品密集时少回表；收益取决于数据分布，不是教条。

## 深翻页怎么治（Q5 的后续）
| 方案 | 耗时 | 说明 |
|---|---|---|
| 原始 `LIMIT 20 OFFSET 100000` | 657 ms | 排序 10 万行 + 全部回表 |
| **延迟关联**（子查询走覆盖索引取 id，再 JOIN 回表） | **43 ms** | 10 万条只在索引里扫，**回表 20 次**；15× |
| **游标分页**（`price < ? OR (price = ? AND id < ?)`） | **1.39 ms** | 读 340 行；**470×**，生产推荐 |

游标分页用行构造器 `(price,id) < (?,?)` 在 MySQL 8.0 能写但优化器不保证走区间，实测用显式展开的 OR 写法才稳定。

## 还没做完的部分（要主动说）

- `idx_status` 和 `idx_category_id` 这两条单列索引现在被复合索引的最左前缀覆盖，**应该 DROP**——`idx_status` 尤其有害，就是它把 Q1 带进了 index merge 的坑。目前只在 `product_bench` 上验证，还没在生产定义 `mall_database.sql` 里落地。
- 慢日志这条路没走完整：本次是用 `EXPLAIN` 直接定位已知热点查询，**不是**从 `slow_query_log` 里捞出来的。别说"我开了慢日志抓到的"，被要求描述日志字段（`Query_time`/`Lock_time`/`Rows_sent`/`Rows_examined`）会答不上。
- `name LIKE '%关键词%'` 前导通配符**永远走不了 B+Tree**，所以站内搜索单独走 Elasticsearch，这是架构选型的理由，别说成"我索引优化了搜索"。
- 20 万行是造的数据。真实 `product` 只有 146 行，146 行下索引没有任何可观测收益——**这一点必须自己先说**，否则被问"你线上表多大"就露了。

## 简历"四类问题"现在全部有物证

项目描述里写的"重点解决秒杀超卖、订单与库存最终一致性、缓存穿透/雪崩、商品仅标题导致语义召不回四类问题"，逐条对应到仓库里的文件：

| 简历声称的问题 | 代码落点 | 量化物证 | 可防守度 |
|---|---|---|---|
| 秒杀超卖 | `mall-seckill-service/.../SeckillService.java`：限流 L91-98、Redisson 锁 L102-105、Lua 预减 L213-218；`CouponMapper.decreaseStock` 乐观锁 | 无压测（只测了读路径） | 中：链路真实，但幂等只有 Redis key、`uk_user_coupon` 唯一索引未建，且限流在 MQ 消费端 |
| 订单与库存最终一致性 | `OrderDelayListener.handleOrderDelay` L72-77 补发资源队列消息（本次新增）+ `RabbitMqConfig` TTL/死信 | 无压测，属功能闭环 | **刚从"假"变"真"**：改动仍在，但 ①手动取消双重还库存 ②消费者无幂等 ③消息发在事务提交前 三处未修 |
| 缓存穿透 / 雪崩 | `ProductService.findById` L234-332 六层防护 | `perf-test/results.json`：DB 查询 9997→37 次、QPS 458→711 | 高：A/B/C 三组对照，`Com_select` 实测，可复现 |
| 商品仅标题导致语义召不回 | `mall-ai-agent/app/rag/tagger.py` L24-31 打标 + `store.py` L506-518 RRF | `mall-ai-agent/README.md` L144-147：Top-3 23.1%→50.0%（33 条自建集，26 条计分，天花板 17/26） | 高：有评测脚本 `eval/run_eval.py` 与 dataset.json |
| **（新增建议）列表页慢查询** | 本目录 `03_add_index.sql` 三条复合索引 | 本文：113ms→0.088ms、深翻页 657ms→1.39ms | 高：EXPLAIN ANALYZE 原文留存 |

**建议**：既然深翻页这一格现在是完整的（索引 → 发现索引不管 offset → 延迟关联 → 游标分页），把它作为**第五个问题**写进项目描述，或者并进缓存那条之后的独立 bullet。这条链路比"我加了个索引"信息量大得多，而且和 AI 那条形成"能定位瓶颈、能量化、知道方案边界"的完整印象。

## 简历那句话（用实测数字替换现有空的表述）

> 负责 MySQL 表结构与慢查询优化：定位 C 端商品列表页因单列索引触发 index merge intersect 与 filesort（20 万行基准表，扫描 18.8 万条索引项、113 ms），依据最左前缀与索引有序性设计复合索引并覆盖 `@TableLogic` 隐式条件，扫描行数降至 20、耗时 0.088 ms（↓99.9%）；进一步以延迟关联/游标分页将深翻页 `OFFSET 100000` 由 657 ms 降至 43 ms / 1.39 ms

替换掉原来的"负责 MySQL 表结构设计与慢查询索引优化"。

## 原始输出

`00_env.txt` / `01_seed.txt` / `02_before.txt` / `03_add_index.txt` / `04_after.txt` / `05_experiments.txt` 是同目录下的完整 `EXPLAIN ANALYZE` 原文，被追问时可以直接翻。
