-- =============================================
-- 批量更新商品图片（一键替换 example.com 占位图）
-- 使用 placehold.co 服务，按商品名自动生成带文字图片
-- 执行方式：source sql/update_product_images.sql 或复制到 Navicat/Datagrip 执行
-- =============================================

-- 1. 更新商品主图（每个商品用不同背景色，便于区分）
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/1a1a2e/e0e0e0?text=iPhone+15+Pro+Max' WHERE `name` = 'iPhone 15 Pro Max';
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/16213e/e0e0e0?text=MacBook+Pro+14' WHERE `name` = 'MacBook Pro 14';
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/0f3460/e0e0e0?text=华为Mate+60+Pro' WHERE `name` = '华为Mate 60 Pro';
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/533483/e0e0e0?text=小米14+Ultra' WHERE `name` = '小米14 Ultra';
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/1a1a2e/e0e0e0?text=ThinkPad+X1+Carbon' WHERE `name` = 'ThinkPad X1 Carbon';
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/2d4059/e0e0e0?text=男士休闲夹克' WHERE `name` = '男士休闲夹克';
UPDATE `product` SET `main_image` = 'https://placehold.co/600x600/9b59b6/e0e0e0?text=女士连衣裙' WHERE `name` = '女士连衣裙';

-- 2. 更新 product_image 表（详情图用不同尺寸/颜色）
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/1a1a2e/e0e0e0?text=iPhone15+Detail+1' WHERE `product_id` = 1 AND `image_type` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/2c3e50/e0e0e0?text=iPhone15+Detail+2' WHERE `product_id` = 1 AND `image_type` = 2 AND `sort` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/34495e/e0e0e0?text=iPhone15+Detail+3' WHERE `product_id` = 1 AND `image_type` = 2 AND `sort` = 2;

UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/16213e/e0e0e0?text=MacBook+Detail+1' WHERE `product_id` = 2 AND `image_type` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/1a1a2e/e0e0e0?text=MacBook+Detail+2' WHERE `product_id` = 2 AND `image_type` = 2;

UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/0f3460/e0e0e0?text=Mate60+Detail+1' WHERE `product_id` = 3 AND `image_type` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/1a3a6b/e0e0e0?text=Mate60+Detail+2' WHERE `product_id` = 3 AND `image_type` = 2;

UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/533483/e0e0e0?text=小米14+Detail+1' WHERE `product_id` = 4 AND `image_type` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/6c5ce7/e0e0e0?text=小米14+Detail+2' WHERE `product_id` = 4 AND `image_type` = 2;

UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/1a1a2e/e0e0e0?text=ThinkPad+Detail' WHERE `product_id` = 5;

UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/2d4059/e0e0e0?text=Jacket+Detail+1' WHERE `product_id` = 6 AND `image_type` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/e67e22/e0e0e0?text=Jacket+Detail+2' WHERE `product_id` = 6 AND `image_type` = 2;

UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/9b59b6/e0e0e0?text=Dress+Detail+1' WHERE `product_id` = 7 AND `image_type` = 1;
UPDATE `product_image` SET `image_url` = 'https://placehold.co/800x800/e91e63/e0e0e0?text=Dress+Detail+2' WHERE `product_id` = 7 AND `image_type` = 2;

-- 3. 更新品牌 logo
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/333/fff?text=Apple' WHERE `name` = 'Apple';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/c41230/fff?text=华为' WHERE `name` = '华为';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/ff6900/fff?text=小米' WHERE `name` = '小米';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/e60012/fff?text=Lenovo' WHERE `name` = 'Lenovo';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/e50010/fff?text=优衣库' WHERE `name` = '优衣库';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/000/fff?text=ZARA' WHERE `name` = 'ZARA';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/1428a0/fff?text=三星' WHERE `name` = '三星';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/1ba784/fff?text=OPPO' WHERE `name` = 'OPPO';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/415fff/fff?text=vivo' WHERE `name` = 'vivo';
UPDATE `brand` SET `logo` = 'https://placehold.co/200x200/0ab9e6/fff?text=荣耀' WHERE `name` = '荣耀';

-- 4. 更新评论图片
UPDATE `product_comment` SET `images` = NULL WHERE `id` = 1;  -- 评论图清空即可，不重要
UPDATE `product_comment` SET `images` = NULL WHERE `id` = 3;
UPDATE `product_comment` SET `images` = NULL WHERE `id` = 5;
UPDATE `product_comment` SET `images` = NULL WHERE `id` = 7;

-- 验证
SELECT id, name, main_image FROM `product`;
SELECT id, product_id, image_url FROM `product_image`;
SELECT id, name, logo FROM `brand`;
