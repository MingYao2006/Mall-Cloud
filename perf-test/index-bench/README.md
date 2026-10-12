# 商品列表页复合索引优化：EXPLAIN 前后对比

配套简历条目："负责 MySQL 表结构设计与索引优化（商品列表页复合索引，消除 filesort）"

## 为什么要单独做这个

线上 `product` 表只有 146 行。146 行时 InnoDB 一定走全表扫描，`EXPLAIN` 里加不加索引结果都一样，
**拿它做前后对比等于没有对比**。所以基准数据造在 `product_bench`（20 万行，表结构和索引由
`CREATE TABLE product_bench LIKE product` 完整复制）。

20 万行的量级依据：按 20 个分类 × 每类 1 万 SKU，是一个中型商城真实商品表的规模，不是随手拍的数。

## 怎么跑

```bat
:: 用 MySQL 的 table 输出，方便直接贴进结果文件
cd /d "D:\Java\project One\perf-test\index-bench"
mysql -uroot -p --table < 00_check_env.sql   > 00_env.txt
mysql -uroot -p --table < 01_seed.sql         > 01_seed.txt
mysql -uroot -p --table < 02_before.sql       > 02_before.txt
mysql -uroot -p --table < 03_add_index.sql    > 03_add_index.txt
mysql -uroot -p --table < 04_after.sql        > 04_after.txt
mysql -uroot -p --table < 05_experiments.sql  > 05_experiments.txt
```

`01_seed.sql` 用递归 CTE 造数据，如果报 `Recursive query aborted`，说明
`cte_max_recursion_depth` 没设上（会话级设置在某些客户端会被重置），重跑一次或改用 workbench 整段执行。

要求 MySQL ≥ 8.0.18（`EXPLAIN ANALYZE`）。低版本把脚本里的 `EXPLAIN ANALYZE` 换成 `EXPLAIN`，
再用 `SET profiling` 或前后各跑 3 次取墙钟时间。

## 优化的是什么查询

来自 `mall-product-service` 的 `ProductService.listProductsByCondition()`，MyBatis-Plus 拼出来的真实形状：

```sql
SELECT * FROM product
WHERE is_deleted = 0            -- 由 @TableLogic 隐式追加，Product.java:111
  AND status = ? AND category_id = ?
  AND price BETWEEN ? AND ?
ORDER BY price DESC
LIMIT ?, ?;
```

优化前 `product` 表只有 5 条单列索引（`idx_category_id`、`idx_price`、`idx_sales`、`idx_status`、`idx_brand`）。

## 三条新索引，各自对应一条真实路径

| 索引 | 服务的查询 | 为什么这么排 |
|---|---|---|
| `(is_deleted, status, category_id, price)` | 分类页 + 价格排序/价格区间 | 三个等值列在前，第四列 price 既做区间又提供有序性 → 消灭 filesort |
| `(is_deleted, status, category_id, sort)` | 分类页默认展示顺序 | 排序列不同，必须单独一条，一条索引服务不了两种 ORDER BY |
| `(is_deleted, status, price)` | 不传分类的全站价格排序 | 实测优化器**没**用它（仍走 `idx_price` 倒序提前终止），但它成了深翻页延迟关联的**覆盖索引** |

## 结果

**已跑完，实测数据与结论见 [results-index-bench.md](./results-index-bench.md)。**
原始 `EXPLAIN ANALYZE` 输出存在同目录的 `02_before.txt` / `04_after.txt` / `05_experiments.txt`。

三个如实记录、不要粉饰的点：

1. **Q5 深翻页加了索引也还是慢**。索引只解决排序，不解决 offset——前 5 万条索引记录照样要扫。
   所以额外做实验 C（延迟关联 / 游标分页），并把它作为"还没做完的部分"讲出来。
2. **`name LIKE '%keyword%'` 永远走不了 B+Tree 索引**（前导通配符），这是搜索单独走 Elasticsearch 的原因，
   别说"我加索引优化了搜索"。
3. **实验 A/B 是负向对照**，目的是证明列顺序不是随手排的。面试官问"为什么不建 `(status, price, category_id)`"，
   你直接给他看实验 A 的执行计划。

## 简历那句话怎么写

跑完拿到真实数字后，把下面这行替换掉现在那句空的"慢查询索引优化"：

> 负责 MySQL 表结构设计与慢查询优化：定位 C 端商品列表页 `ORDER BY price` 触发 filesort（20 万行基准表，优化前扫描 X 行 / Y ms），按最左前缀与索引序设计 3 条复合索引并考虑 `@TableLogic` 隐式条件，扫描行数降至 20 行、响应降至 Z ms；并用延迟关联解决深翻页 OFFSET 退化问题

X/Y/Z 必须是你自己跑出来的数。**填别人的数，被追问执行计划细节会当场崩。**

## 面试问答准备

- **"为什么把 is_deleted 放最左？它基数只有 0/1。"**
  不是为了过滤，是为了让等值前缀连续：B+Tree 索引只有前面所有列都是等值时，后面的列才天然有序，才能省掉 filesort。`@TableLogic` 保证这条等值条件每条查询必带，放最左零成本。
- **"低基数列到底要不要放索引？"** → 看实验 B 的结论：放与不放都能用索引序，差别在回表次数和覆盖能力。
- **"加了这么多索引，写性能呢？"** → 商品表读多写少（B 端改价才写），3 条复合索引 + 原有 5 条单列，写放大可以接受；真正该做的是**删冗余索引**——`idx_status` 和 `idx_category_id` 现在已被复合索引的最左列覆盖/取代，可以 DROP，这点要主动说。
- **"怎么发现的？"** → 说 `slow_query_log`（`long_query_time=0.1`）+ `EXPLAIN`，不要说"我猜的"。本地开慢日志：
  `SET GLOBAL slow_query_log = 1; SET GLOBAL long_query_time = 0.1;`
