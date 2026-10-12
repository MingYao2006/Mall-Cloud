-- 02_before.sql
-- 【BEFORE】只有 mall_database.sql 里的单列索引：idx_category_id / idx_price / idx_sales / idx_status
-- 依次执行，把每条的 EXPLAIN ANALYZE 输出保存下来（mysql --table 模式或 workbench 截图）
USE mall;

-- 先空跑一次热缓冲池，第二次的数据才算数
SELECT id FROM product_bench WHERE status=1 AND category_id=7 ORDER BY price DESC LIMIT 20;

-- ---------------------------------------------------------------
-- Q1 分类页默认排序：对应 ProductService.listProductsByCondition 的 orderBy=price DESC
-- 真实 SQL（MyBatis-Plus 会替 @TableLogic 隐式追加 is_deleted=0）：
-- ---------------------------------------------------------------
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
-- 预期看到的问题：优化器只能二选一——走 idx_category_id 命中 ~1 万行后 filesort，
-- 或走 idx_price 倒序扫并逐行过滤 category_id（rows 估算巨大）。Extra 里出现 Using filesort。

-- ---------------------------------------------------------------
-- Q2 分类页按 sort 升序（前台默认展示顺序）
-- ---------------------------------------------------------------
EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
ORDER BY sort ASC
LIMIT 20 OFFSET 0;

-- ---------------------------------------------------------------
-- Q3 价格区间 + 价格升序（筛选面板：分类 + 500~3000 元 + 价格从低到高）
-- ---------------------------------------------------------------
EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1 AND category_id = 7
  AND price BETWEEN 500 AND 3000
ORDER BY price ASC
LIMIT 20 OFFSET 0;

-- ---------------------------------------------------------------
-- Q4 全站按价格排序（不传 category_id）
-- ---------------------------------------------------------------
EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1
ORDER BY price DESC
LIMIT 20 OFFSET 0;

-- ---------------------------------------------------------------
-- Q5 深翻页（第 5000 页）—— 走全站按价格排序路径
-- 注意：单分类只有 1 万行，OFFSET 5 万会直接返回空集、测不出退化，
-- 所以深翻页要拿 status=1 的 18.8 万行路径测
-- ---------------------------------------------------------------
EXPLAIN ANALYZE
SELECT * FROM product_bench
WHERE is_deleted = 0 AND status = 1
ORDER BY price DESC
LIMIT 20 OFFSET 100000;

SHOW INDEX FROM product_bench;
