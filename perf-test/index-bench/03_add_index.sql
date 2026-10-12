-- 03_add_index.sql
-- 【优化动作】为 C 端商品列表页的三条真实查询建复合索引
USE mall;

-- 覆盖 Q1/Q3：等值(is_deleted, status, category_id) 在前，排序/区间列 price 在后
-- 这样索引本身的有序性直接满足 ORDER BY price，消灭 filesort
ALTER TABLE product_bench ADD INDEX idx_list_status_cat_price (is_deleted, status, category_id, price);

-- 覆盖 Q2：同样的等值前缀，排序列换成 sort
ALTER TABLE product_bench ADD INDEX idx_list_status_cat_sort  (is_deleted, status, category_id, sort);

-- 覆盖 Q4（不分类、全站按价格排）：没有 category_id 时，
-- 上面两个索引的第四列 price/sort 无法用于排序（第三列断层），所以必须单独建一条短索引
ALTER TABLE product_bench ADD INDEX idx_list_status_price     (is_deleted, status, price);

ANALYZE TABLE product_bench;

SHOW INDEX FROM product_bench;
