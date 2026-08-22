-- =============================================
-- 扩充商品数据（30+ 商品，覆盖三大品类）
-- 执行方式：先执行 mall_database.sql 再执行本脚本
-- =============================================

USE mall;

-- =============================================
-- 1. 新增子分类（家用电器下）
-- =============================================
INSERT INTO `category` (`name`, `parent_id`, `level`, `sort`, `status`) VALUES
('大家电', 3, 2, 1, 1),
('小家电', 3, 2, 2, 1),
('厨房电器', 3, 2, 3, 1),
('智能穿戴', 1, 2, 3, 1);   -- 数码产品下新增

-- =============================================
-- 2. 新增品牌
-- =============================================
INSERT INTO `brand` (`name`, `logo`, `description`, `sort`, `status`) VALUES
('海尔', 'https://placehold.co/200x200/1a73e8/fff?text=海尔', '海尔集团，全球知名家电品牌', 11, 1),
('格力', 'https://placehold.co/200x200/0066cc/fff?text=格力', '格力电器，掌握核心科技', 12, 1),
('美的', 'https://placehold.co/200x200/8e44ad/fff?text=美的', '美的集团，智慧生活解决方案提供商', 13, 1),
('戴森', 'https://placehold.co/200x200/2c3e50/fff?text=Dyson', '戴森，英国高端家电品牌', 14, 1),
('飞利浦', 'https://placehold.co/200x200/003399/fff?text=飞利浦', '飞利浦，荷兰皇家品牌', 15, 1),
('九阳', 'https://placehold.co/200x200/e67e22/fff?text=九阳', '九阳，厨房电器领导品牌', 16, 1),
('Samsung', 'https://placehold.co/200x200/1428a0/fff?text=三星', '三星电子，全球科技巨头', 17, 1),
('Apple', 'https://placehold.co/200x200/333/fff?text=Apple', '苹果公司，全球知名科技品牌', 1, 1),
('华为', 'https://placehold.co/200x200/c41230/fff?text=华为', '华为技术有限公司，全球领先的ICT解决方案提供商', 2, 1),
('小米', 'https://placehold.co/200x200/ff6900/fff?text=小米', '小米科技有限责任公司', 3, 1),
('Lenovo', 'https://placehold.co/200x200/e60012/fff?text=Lenovo', '联想集团，全球PC领导企业', 4, 1),
('优衣库', 'https://placehold.co/200x200/e50010/fff?text=优衣库', '日本快时尚品牌', 5, 1),
('ZARA', 'https://placehold.co/200x200/000/fff?text=ZARA', '西班牙快时尚品牌', 6, 1),
('OPPO', 'https://placehold.co/200x200/1ba784/fff?text=OPPO', 'OPPO，知名手机品牌', 7, 1),
('vivo', 'https://placehold.co/200x200/415fff/fff?text=vivo', 'vivo，知名手机品牌', 8, 1),
('荣耀', 'https://placehold.co/200x200/0ab9e6/fff?text=荣耀', '荣耀，年轻科技潮牌', 9, 1),
('Nike', 'https://placehold.co/200x200/000/fff?text=Nike', '耐克，全球运动品牌', 18, 1),
('Adidas', 'https://placehold.co/200x200/000/fff?text=Adidas', '阿迪达斯，全球运动品牌', 19, 1),
('ONLY', 'https://placehold.co/200x200/ff69b4/fff?text=ONLY', 'ONLY，欧洲时尚女装品牌', 20, 1),
('海尔', 'https://placehold.co/200x200/1a73e8/fff?text=海尔', '海尔集团，全球知名家电品牌', 21, 1),
('格力', 'https://placehold.co/200x200/0066cc/fff?text=格力', '格力电器，掌握核心科技', 22, 1),
('美的', 'https://placehold.co/200x200/8e44ad/fff?text=美的', '美的集团，智慧生活解决方案提供商', 23, 1)
ON DUPLICATE KEY UPDATE `logo` = VALUES(`logo`);

-- =============================================
-- 3. 大量新增商品（30+ 件）
-- =============================================

-- ---- 手机通讯 (category_id=4) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('Samsung Galaxy S24 Ultra', 4, 'Samsung', 'https://placehold.co/600x600/1428a0/fff?text=Galaxy+S24+Ultra', 9699.00, 10999.00, 80, 430, 1),
('OPPO Find X7 Ultra', 4, 'OPPO', 'https://placehold.co/600x600/1ba784/fff?text=Find+X7+Ultra', 5999.00, 6499.00, 120, 380, 1),
('vivo X100 Pro', 4, 'vivo', 'https://placehold.co/600x600/415fff/fff?text=vivo+X100+Pro', 4999.00, 5499.00, 150, 560, 1),
('荣耀 Magic6 Pro', 4, '荣耀', 'https://placehold.co/600x600/0ab9e6/fff?text=Magic6+Pro', 5699.00, 6199.00, 100, 290, 1);

-- ---- 电脑办公 (category_id=5) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('iPad Pro 12.9 M4', 5, 'Apple', 'https://placehold.co/600x600/333/fff?text=iPad+Pro+M4', 8999.00, 9499.00, 60, 210, 1),
('华为 MatePad Pro 13.2', 5, '华为', 'https://placehold.co/600x600/c41230/fff?text=MatePad+Pro', 5499.00, 5999.00, 80, 180, 1),
('小米平板 6S Pro', 5, '小米', 'https://placehold.co/600x600/ff6900/fff?text=小米平板', 3299.00, 3599.00, 100, 350, 1),
('ThinkBook 16+', 5, 'Lenovo', 'https://placehold.co/600x600/e60012/fff?text=ThinkBook', 6499.00, 6999.00, 70, 140, 1);

-- ---- 智能穿戴 (category_id=8) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('Apple Watch Ultra 2', 8, 'Apple', 'https://placehold.co/600x600/333/fff?text=Watch+Ultra+2', 6499.00, 6999.00, 40, 120, 1),
('华为 Watch GT 4', 8, '华为', 'https://placehold.co/600x600/c41230/fff?text=Watch+GT4', 2488.00, 2688.00, 60, 280, 1),
('小米手环 8 Pro', 8, '小米', 'https://placehold.co/600x600/ff6900/fff?text=手环8+Pro', 399.00, 449.00, 200, 920, 1);

-- ---- 男装 (category_id=6) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('Nike Air 运动鞋', 6, 'Nike', 'https://placehold.co/600x600/000/fff?text=Nike+Air', 899.00, 1099.00, 300, 1500, 1),
('Adidas 运动卫衣', 6, 'Adidas', 'https://placehold.co/600x600/111/fff?text=Adidas+卫衣', 459.00, 599.00, 200, 680, 1),
('男士商务皮鞋', 6, '优衣库', 'https://placehold.co/600x600/e50010/fff?text=商务皮鞋', 599.00, 799.00, 150, 420, 1),
('男士轻薄羽绒服', 6, '优衣库', 'https://placehold.co/600x600/e50010/fff?text=羽绒服', 499.00, 699.00, 180, 890, 1);

-- ---- 女装 (category_id=7) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('ONLY 春季风衣', 7, 'ONLY', 'https://placehold.co/600x600/ff69b4/fff?text=春季风衣', 799.00, 999.00, 120, 360, 1),
('ZARA 印花半身裙', 7, 'ZARA', 'https://placehold.co/600x600/000/fff?text=印花半身裙', 359.00, 459.00, 180, 520, 1),
('女士真丝衬衫', 7, 'ZARA', 'https://placehold.co/600x600/000/fff?text=真丝衬衫', 399.00, 499.00, 140, 410, 1),
('女士高腰阔腿裤', 7, 'ONLY', 'https://placehold.co/600x600/ff69b4/fff?text=阔腿裤', 329.00, 429.00, 200, 680, 1);

-- ---- 大家电 (category_id=9) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('海尔 500L 对开门冰箱', 9, '海尔', 'https://placehold.co/600x600/1a73e8/fff?text=海尔冰箱', 3999.00, 4599.00, 50, 320, 1),
('格力 1.5匹 变频空调', 9, '格力', 'https://placehold.co/600x600/0066cc/fff?text=格力空调', 3299.00, 3699.00, 60, 480, 1),
('美的 10公斤 滚筒洗衣机', 9, '美的', 'https://placehold.co/600x600/8e44ad/fff?text=美的洗衣机', 2699.00, 3199.00, 40, 260, 1),
('海尔 65寸 智能电视', 9, '海尔', 'https://placehold.co/600x600/1a73e8/fff?text=海尔电视', 4299.00, 4999.00, 30, 150, 1);

-- ---- 小家电 (category_id=10) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('戴森 V15 无线吸尘器', 10, '戴森', 'https://placehold.co/600x600/2c3e50/fff?text=Dyson+V15', 4990.00, 5490.00, 35, 190, 1),
('飞利浦 电动剃须刀 S9000', 10, '飞利浦', 'https://placehold.co/600x600/003399/fff?text=飞利浦+剃须刀', 1499.00, 1799.00, 80, 460, 1),
('小米 空气净化器 Pro', 10, '小米', 'https://placehold.co/600x600/ff6900/fff?text=空气净化器', 1299.00, 1499.00, 70, 340, 1),
('戴森 吹风机 Supersonic', 10, '戴森', 'https://placehold.co/600x600/2c3e50/fff?text=戴森+吹风机', 3190.00, 3490.00, 45, 280, 1);

-- ---- 厨房电器 (category_id=11) ----
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('美的 变频微波炉 23L', 11, '美的', 'https://placehold.co/600x600/8e44ad/fff?text=美的微波炉', 599.00, 699.00, 90, 520, 1),
('九阳 免洗豆浆机 DJ12B', 11, '九阳', 'https://placehold.co/600x600/e67e22/fff?text=九阳豆浆机', 699.00, 799.00, 70, 380, 1),
('美的 电饭煲 5L', 11, '美的', 'https://placehold.co/600x600/8e44ad/fff?text=美的电饭煲', 459.00, 529.00, 100, 650, 1),
('九阳 空气炸锅 5.5L', 11, '九阳', 'https://placehold.co/600x600/e67e22/fff?text=九阳空气炸锅', 399.00, 479.00, 120, 720, 1);

-- =============================================
-- 4. 产品图片
-- =============================================
INSERT INTO `product_image` (`product_id`, `image_url`, `image_type`, `sort`) VALUES
-- Galaxy S24 (id=8)
(8, 'https://placehold.co/800x800/1428a0/fff?text=Galaxy+S24+Front', 1, 1),
(8, 'https://placehold.co/800x800/1428a0/fff?text=Galaxy+S24+Back', 2, 1),
-- OPPO Find X7 (id=9)
(9, 'https://placehold.co/800x800/1ba784/fff?text=Find+X7+Front', 1, 1),
(9, 'https://placehold.co/800x800/1ba784/fff?text=Find+X7+Back', 2, 1),
-- vivo X100 (id=10)
(10, 'https://placehold.co/800x800/415fff/fff?text=vivo+X100+Front', 1, 1),
-- 荣耀 Magic6 (id=11)
(11, 'https://placehold.co/800x800/0ab9e6/fff?text=Magic6+Pro', 1, 1),
-- iPad Pro (id=12)
(12, 'https://placehold.co/800x800/333/fff?text=iPad+Pro+Front', 1, 1),
(12, 'https://placehold.co/800x800/333/fff?text=iPad+Pro+Side', 2, 1),
-- MatePad (id=13)
(13, 'https://placehold.co/800x800/c41230/fff?text=MatePad+Pro', 1, 1),
-- 小米平板 (id=14)
(14, 'https://placehold.co/800x800/ff6900/fff?text=小米平板6S', 1, 1),
-- ThinkBook (id=15)
(15, 'https://placehold.co/800x800/e60012/fff?text=ThinkBook', 1, 1),
-- Apple Watch (id=16)
(16, 'https://placehold.co/800x800/333/fff?text=Watch+Ultra', 1, 1),
-- 华为手表 (id=17)
(17, 'https://placehold.co/800x800/c41230/fff?text=Watch+GT4', 1, 1),
-- 小米手环 (id=18)
(18, 'https://placehold.co/800x800/ff6900/fff?text=Mi+Band+8', 1, 1),
-- Nike 鞋 (id=19)
(19, 'https://placehold.co/800x800/000/fff?text=Nike+Air+Side', 1, 1),
(19, 'https://placehold.co/800x800/000/fff?text=Nike+Air+Top', 2, 1),
-- Adidas 卫衣 (id=20)
(20, 'https://placehold.co/800x800/111/fff?text=Adidas+Sweater', 1, 1),
-- 商务皮鞋 (id=21)
(21, 'https://placehold.co/800x800/e50010/fff?text=商务皮鞋', 1, 1),
-- 羽绒服 (id=22)
(22, 'https://placehold.co/800x800/e50010/fff?text=羽绒服', 1, 1),
-- ONLY 风衣 (id=23)
(23, 'https://placehold.co/800x800/ff69b4/fff?text=ONLY+风衣', 1, 1),
-- ZARA 半身裙 (id=24)
(24, 'https://placehold.co/800x800/000/fff?text=印花半身裙', 1, 1),
-- 真丝衬衫 (id=25)
(25, 'https://placehold.co/800x800/000/fff?text=真丝衬衫', 1, 1),
-- 阔腿裤 (id=26)
(26, 'https://placehold.co/800x800/ff69b4/fff?text=阔腿裤', 1, 1),
-- 海尔冰箱 (id=27)
(27, 'https://placehold.co/800x800/1a73e8/fff?text=海尔冰箱+Open', 1, 1),
-- 格力空调 (id=28)
(28, 'https://placehold.co/800x800/0066cc/fff?text=格力空调', 1, 1),
-- 美的洗衣机 (id=29)
(29, 'https://placehold.co/800x800/8e44ad/fff?text=美的洗衣机', 1, 1),
-- 海尔电视 (id=30)
(30, 'https://placehold.co/800x800/1a73e8/fff?text=海尔电视', 1, 1),
-- 戴森吸尘器 (id=31)
(31, 'https://placehold.co/800x800/2c3e50/fff?text=Dyson+V15+Side', 1, 1),
-- 飞利浦剃须刀 (id=32)
(32, 'https://placehold.co/800x800/003399/fff?text=飞利浦剃须刀', 1, 1),
-- 小米净化器 (id=33)
(33, 'https://placehold.co/800x800/ff6900/fff?text=空气净化器', 1, 1),
-- 戴森吹风机 (id=34)
(34, 'https://placehold.co/800x800/2c3e50/fff?text=戴森吹风机', 1, 1),
-- 美的微波炉 (id=35)
(35, 'https://placehold.co/800x800/8e44ad/fff?text=美的微波炉', 1, 1),
-- 九阳豆浆机 (id=36)
(36, 'https://placehold.co/800x800/e67e22/fff?text=九阳豆浆机', 1, 1),
-- 美的电饭煲 (id=37)
(37, 'https://placehold.co/800x800/8e44ad/fff?text=美的电饭煲', 1, 1),
-- 九阳空气炸锅 (id=38)
(38, 'https://placehold.co/800x800/e67e22/fff?text=九阳空气炸锅', 1, 1);

-- =============================================
-- 5. 商品评价
-- =============================================
INSERT INTO `product_comment` (`product_id`, `user_id`, `content`, `rating`, `status`) VALUES
(8, 2, '三星屏幕真的绝了，拍照也超棒！', 5, 1),
(8, 1, '价格有点贵但确实好用', 4, 1),
(9, 2, 'OPPO拍照效果太惊艳了', 5, 1),
(10, 1, 'vivo性价比很高，系统流畅', 4, 1),
(12, 1, 'iPad Pro M4性能怪兽，剪视频流畅', 5, 1),
(14, 2, '小米平板价格实惠，够用', 4, 1),
(16, 2, 'Apple Watch 续航有提升', 4, 1),
(19, 1, 'Nike Air 穿着很舒服，版型好看', 5, 1),
(20, 2, 'Adidas卫衣质量很好，保暖', 4, 1),
(23, 1, 'ONLY风衣款式好看，老婆很喜欢', 5, 1),
(24, 2, 'ZARA裙子性价比高', 4, 1),
(27, 1, '海尔冰箱容量大，制冷效果好', 5, 1),
(28, 2, '格力空调制冷快，噪音小', 5, 1),
(31, 1, '戴森吸尘器吸力真的强', 5, 1),
(35, 2, '美的微波炉加热均匀，好评', 4, 1),
(36, 1, '九阳豆浆机方便快捷，早餐神器', 5, 1),
(38, 2, '空气炸锅炸鸡翅很香', 4, 1);

-- =============================================
-- 验证
-- =============================================
SELECT '=== 分类数量 ===' AS '';
SELECT CONCAT(c1.name, ' → ', COALESCE(c2.name, '(无子分类)')) AS 分类结构,
       (SELECT COUNT(*) FROM product WHERE category_id = c2.id) AS 商品数
FROM category c1
LEFT JOIN category c2 ON c2.parent_id = c1.id
WHERE c1.parent_id = 0 AND c1.is_deleted = 0
ORDER BY c1.sort, c2.sort;

SELECT '=== 总商品数 ===' AS '';
SELECT COUNT(*) AS 总商品数 FROM product WHERE is_deleted = 0;
