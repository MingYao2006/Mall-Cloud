-- 01_seed.sql
-- 造 20 万行商品数据，结构/索引与线上 product 表完全一致（CREATE TABLE ... LIKE 会连同单列索引一起复制）
-- 数据分布刻意做成"确定性"的（不用 RAND()），这样 before/after 两次跑的结果可复现、可对比，
-- 面试官问"你怎么保证前后对比公平"时，这就是答案。
USE mall;

DROP TABLE IF EXISTS product_bench;
CREATE TABLE product_bench LIKE product;

-- 放开递归 CTE 深度限制，允许一次生成 20 万行
SET SESSION cte_max_recursion_depth = 300000;

INSERT INTO product_bench
    (id, name, category_id, brand, main_image, price, original_price, stock, sales, status, sort, create_time, update_time, is_deleted)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200000
)
SELECT
    n,
    CONCAT('商品-', LPAD(n, 7, '0')),
    1 + (n % 20),                                        -- 20 个分类，均匀分布
    ELT(1 + (n % 8), 'Apple', 'Xiaomi', 'Huawei', 'Nike', 'Adidas', 'Uniqlo', 'Sony', 'Haier'),
    CONCAT('/img/', n % 20, '/', n, '.jpg'),
    ROUND(9.9 + ((n * 37) % 20000), 2),                  -- 价格 9.9 ~ 19999.9，均匀
    ROUND(9.9 + ((n * 37) % 20000) * 1.15, 2),
    (n * 11) % 500,
    (n * 13) % 10000,                                    -- 销量
    CASE WHEN n % 20 = 0 THEN 0                          -- 5% 下架
         WHEN n % 50 = 0 THEN 2                          -- 2% 售罄
         ELSE 1 END,                                     -- 93% 上架
    n % 100,                                             -- sort 值域 0~99，重复度高
    DATE_ADD('2025-01-01', INTERVAL (n % 700) DAY),
    DATE_ADD('2025-01-01', INTERVAL (n % 700) DAY),
    0
FROM seq;

-- 造数据阶段保持 detail 为 NULL：与线上真实情况一致（146 条里 145 条 detail 为空）
-- 让优化器看到的数据分布接近真实商城：上架商品占绝大多数
ANALYZE TABLE product_bench;

SELECT
    COUNT(*) AS total_rows,
    SUM(status = 1) AS status_on_sale,
    COUNT(DISTINCT category_id) AS categories,
    MIN(price) AS min_price,
    MAX(price) AS max_price
FROM product_bench;
-- 期望输出：200000 / 186000 / 20 / 9.90 / 19999.90
