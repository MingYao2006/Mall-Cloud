-- 05_experiments.sql
-- 三个对照实验。这三个才是面试官真正想听的——不是"我加了索引变快了"，而是"我知道为什么这样建"
USE mall;

-- ===============================================================
-- 实验 A：列顺序错了会怎样？
-- 建一条 (is_deleted, status, price, category_id)：把排序列放在 category_id 前面
-- ===============================================================
ALTER TABLE product_bench ADD INDEX idx_wrong_order (is_deleted, status, price, category_id);
ANALYZE TABLE product_bench;

-- 强制用这条"错顺序"的索引
EXPLAIN ANALYZE
SELECT * FROM product_bench FORCE INDEX (idx_wrong_order)
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
ORDER BY price DESC
LIMIT 20 OFFSET 0;
-- 结论：price 能做索引范围/有序，但 category_id 退化成 Index filter（回到表前不过滤掉），
-- 需要读的索引条目数暴涨，Extra 里 Using index condition; Using where。
-- 这就是"最左前缀 + 等值列必须在排序列之前"的实证。

ALTER TABLE product_bench DROP INDEX idx_wrong_order;

-- ===============================================================
-- 实验 B：status 基数极低（只有 0/1/2），值不值得放进索引？
-- 建一条不含 status 的 (is_deleted, category_id, price) 对比
-- ===============================================================
ALTER TABLE product_bench ADD INDEX idx_no_status (is_deleted, category_id, price);
ANALYZE TABLE product_bench;

EXPLAIN ANALYZE
SELECT * FROM product_bench FORCE INDEX (idx_no_status)
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
ORDER BY price DESC
LIMIT 20 OFFSET 0;
-- 结论：依然能用索引序，status 变成回表后的过滤条件。
-- 差别在于：把 status 放进索引，5% 的下架商品在索引层就被排除，
-- 遇到下架密集的页码时能少回很多次表；代价是索引更大、写放大。
-- 面试话术：低基数列不是为了"缩小扫描范围"而放前面，是为了"让等值前缀连续，从而保住索引序 + 支持覆盖"。

ALTER TABLE product_bench DROP INDEX idx_no_status;

-- ===============================================================
-- 实验 C：深翻页怎么治（Q5 的后续）
-- ===============================================================
-- 写法 1：延迟关联（先走覆盖索引拿 id，再回表）
EXPLAIN ANALYZE
SELECT p.* FROM product_bench p
JOIN (
    SELECT id FROM product_bench
    WHERE is_deleted = 0 AND status = 1
    ORDER BY price DESC
    LIMIT 20 OFFSET 100000
) t ON p.id = t.id
ORDER BY p.price DESC;
-- 子查询是覆盖索引（Extra: Using index），不再 10 万次回表，只有 20 次回表。

-- 写法 2：游标分页（生产推荐）——用上一页最后一行的 price+id 当前游标
-- 注意：行构造器 (price,id) < (?,?) 在 MySQL 8.0 能写，但优化器不保证走索引区间，
-- 更稳的写法是显式展开成 OR：
EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
  AND (price < 12345.67 OR (price = 12345.67 AND id < 60000))
ORDER BY price DESC, id DESC
LIMIT 20;
-- 若这条 EXPLAIN 仍出现 range 失效，就在 README 结论里如实写"游标分页需要配合 ORDER BY 与索引列完全一致"

SHOW INDEX FROM product_bench;
