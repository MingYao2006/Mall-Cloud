-- 00_check_env.sql
-- 跑之前先执行这个，确认环境和数据量
USE mall;

SELECT VERSION() AS mysql_version;
-- EXPLAIN ANALYZE 需要 MySQL 8.0.18+；低于此版本请把 02/04 里的 EXPLAIN ANALYZE 换成普通 EXPLAIN

SELECT COUNT(*) AS product_rows_real FROM product;
-- 真实表只有 146 行，146 行时优化器一定会选全表扫描，索引对比毫无意义，
-- 所以基准测试必须在造数据的大表上做。

SHOW VARIABLES LIKE 'cte_max_recursion_depth';
