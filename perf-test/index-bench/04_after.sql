-- 04_after.sql
-- 【AFTER】与 02_before.sql 完全相同的五条查询，逐条对比
USE mall;

SELECT id FROM product_bench WHERE status=1 AND category_id=7 ORDER BY price DESC LIMIT 20;

EXPLAIN FORMAT=TREE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
ORDER BY price DESC
LIMIT 20 OFFSET 0;

EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
ORDER BY price DESC
LIMIT 20 OFFSET 0;
-- 预期：key = idx_list_status_cat_price，Extra 无 Using filesort，
-- actual rows ≈ 20（而不是 ~1 万），因为索引序 + LIMIT 可以提前终止

EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
ORDER BY sort ASC
LIMIT 20 OFFSET 0;

EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
  AND price BETWEEN 500 AND 3000
ORDER BY price ASC
LIMIT 20 OFFSET 0;

EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1
ORDER BY price DESC
LIMIT 20 OFFSET 0;

EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1
ORDER BY price DESC
LIMIT 20 OFFSET 100000;
-- Q5 深翻页预期仍然慢：索引只解决了排序，offset 前的 10 万条索引记录照样要扫并逐条回表。
-- 这是另一个优化点（见 05_experiments.sql 的延迟关联写法），面试常考。

SHOW INDEX FROM product_bench;
