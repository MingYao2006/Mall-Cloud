-- 06_cleanup.sql
-- 基准测试做完后清理，别把 20 万行造的数据留在仓库对应的库里（会干扰业务功能演示）
USE mall;

-- 如果还想保留结论，先把 02/04 的输出存成文本再删表
DROP TABLE IF EXISTS product_bench;

-- 真实 product 表（146 行）要不要也加复合索引？
-- 建议加，理由不是性能（146 行怎么走都一样），而是让简历上"索引优化"这句
-- 在你自己的库里有落点。加完记得同步进 mall_database.sql 的建表语句。
ALTER TABLE product
    ADD INDEX idx_list_status_cat_price (is_deleted, status, category_id, price),
    ADD INDEX idx_list_status_cat_sort  (is_deleted, status, category_id, sort),
    ADD INDEX idx_list_status_price     (is_deleted, status, price);
ANALYZE TABLE product;
