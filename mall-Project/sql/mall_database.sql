-- =============================================
-- 网购商城数据库表结构设计
-- 作者：明耀
-- 创建时间：2026-05-09
-- 数据库：MySQL 8.0+
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mall;

-- =============================================
-- 1. 用户表 (user)
-- =============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 2. 商品分类表 (category)
-- =============================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID：0表示一级分类',
    `level` TINYINT DEFAULT 1 COMMENT '分类层级：1-一级，2-二级，3-三级',
    `sort` INT DEFAULT 0 COMMENT '排序：数字越小越靠前',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- =============================================
-- 3. 商品表 (product)
-- =============================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `brand` VARCHAR(100) DEFAULT NULL COMMENT '品牌',
    `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图URL',
    `sub_images` TEXT DEFAULT NULL COMMENT '副图URL，多个用逗号分隔',
    `detail` TEXT DEFAULT NULL COMMENT '商品详情（富文本）',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '价格（元）',
    `original_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '原价（元）',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales` INT DEFAULT 0 COMMENT '销量',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-下架，1-上架，2-售罄',
    `sort` INT DEFAULT 0 COMMENT '排序：数字越小越靠前',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_brand` (`brand`),
    KEY `idx_price` (`price`),
    KEY `idx_sales` (`sales`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- =============================================
-- 4. 购物车表 (cart)
-- =============================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '商品数量',
    `selected` TINYINT DEFAULT 1 COMMENT '是否选中：0-未选中，1-已选中',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`) COMMENT '同一用户同一商品只能有一条记录',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- =============================================
-- 5. 订单表 (orders)
-- =============================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    `pay_amount` DECIMAL(10, 2) DEFAULT NULL COMMENT '实付金额',
    `freight_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '运费',
    `discount_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠金额',
    `payment_type` TINYINT DEFAULT 1 COMMENT '支付方式：1-微信，2-支付宝，3-银行卡',
    `status` TINYINT DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消，5-已退款',
    `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间',
    `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(11) NOT NULL COMMENT '收货人电话',
    `receiver_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- =============================================
-- 6. 订单详情表 (order_item)
-- =============================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单详情ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称（冗余字段）',
    `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品主图（冗余字段）',
    `product_price` DECIMAL(10, 2) NOT NULL COMMENT '商品单价（下单时的价格）',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `total_price` DECIMAL(10, 2) NOT NULL COMMENT '小计金额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单详情表';

-- =============================================
-- 初始化测试数据
-- =============================================

-- 插入测试用户
INSERT INTO `user` (`username`, `password`, `phone`, `email`, `gender`, `status`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', 'admin@mall.com', 1, 1, 1),
('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138001', 'test@mall.com', 2, 1, 0);
-- 说明：密码都是 123456（BCrypt加密）

-- 插入商品分类
INSERT INTO `category` (`name`, `parent_id`, `level`, `sort`, `status`) VALUES
('数码产品', 0, 1, 1, 1),
('服装鞋帽', 0, 1, 2, 1),
('家用电器', 0, 1, 3, 1),
('手机通讯', 1, 2, 1, 1),
('电脑办公', 1, 2, 2, 1),
('男装', 2, 2, 1, 1),
('女装', 2, 2, 2, 1);

-- 插入测试商品
INSERT INTO `product` (`name`, `category_id`, `brand`, `main_image`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
('iPhone 15 Pro Max', 4, 'Apple', 'https://example.com/product/iphone15_1.jpg', 9999.00, 10999.00, 100, 520, 1),
('MacBook Pro 14', 5, 'Apple', 'https://example.com/product/macbook_1.jpg', 14999.00, 15999.00, 50, 280, 1),
('华为Mate 60 Pro', 4, '华为', 'https://example.com/product/mate60_1.jpg', 6999.00, 7999.00, 200, 890, 1),
('小米14 Ultra', 4, '小米', 'https://example.com/product/xiaomi14_1.jpg', 5999.00, 6499.00, 150, 650, 1),
('ThinkPad X1 Carbon', 5, 'Lenovo', 'https://example.com/product/thinkpad_1.jpg', 11999.00, 12999.00, 80, 180, 1),
('男士休闲夹克', 6, '优衣库', 'https://example.com/product/jacket_1.jpg', 299.00, 399.00, 500, 1200, 1),
('女士连衣裙', 7, 'ZARA', 'https://example.com/product/dress_1.jpg', 459.00, 599.00, 300, 850, 1);

-- =============================================
-- 7. 品牌表 (brand)
-- =============================================
DROP TABLE IF EXISTS `brand`;
CREATE TABLE `brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '品牌ID',
    `name` VARCHAR(100) NOT NULL COMMENT '品牌名称',
    `logo` VARCHAR(255) DEFAULT NULL COMMENT '品牌Logo URL',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '品牌描述',
    `sort` INT DEFAULT 0 COMMENT '排序：数字越小越靠前',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_status` (`status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌表';

-- =============================================
-- 8. 商品图片表 (product_image)
-- =============================================
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `image_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
    `image_type` TINYINT DEFAULT 1 COMMENT '图片类型：1-主图，2-详情图，3-规格图',
    `sort` INT DEFAULT 0 COMMENT '排序：数字越小越靠前',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_image_type` (`image_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

-- =============================================
-- 9. 商品SKU规格表 (product_sku)
-- =============================================
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_code` VARCHAR(100) NOT NULL COMMENT 'SKU编码',
    `sku_name` VARCHAR(200) DEFAULT NULL COMMENT 'SKU名称',
    `spec_json` JSON DEFAULT NULL COMMENT '规格属性JSON，如：{"颜色":"黑色","尺寸":"XL"}',
    `price` DECIMAL(10, 2) NOT NULL COMMENT 'SKU价格',
    `original_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '原价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales` INT DEFAULT 0 COMMENT '销量',
    `image` VARCHAR(255) DEFAULT NULL COMMENT 'SKU图片',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU规格表';

-- =============================================
-- 10. 商品参数表 (product_param)
-- =============================================
DROP TABLE IF EXISTS `product_param`;
CREATE TABLE `product_param` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '参数ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `param_name` VARCHAR(100) NOT NULL COMMENT '参数名称',
    `param_value` VARCHAR(500) NOT NULL COMMENT '参数值',
    `sort` INT DEFAULT 0 COMMENT '排序：数字越小越靠前',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_param_name` (`param_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品参数表';

-- =============================================
-- 11. 商品评价表 (product_comment)
-- =============================================
DROP TABLE IF EXISTS `product_comment`;
CREATE TABLE `product_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '订单ID',
    `sku_id` BIGINT DEFAULT NULL COMMENT 'SKU ID',
    `content` TEXT NOT NULL COMMENT '评价内容',
    `images` TEXT DEFAULT NULL COMMENT '评价图片URL，多个用逗号分隔',
    `rating` TINYINT DEFAULT 5 COMMENT '评分：1-5星',
    `is_anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
    `reply` TEXT DEFAULT NULL COMMENT '商家回复内容',
    `reply_time` DATETIME DEFAULT NULL COMMENT '商家回复时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-隐藏，1-显示',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_rating` (`rating`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品评价表';

-- =============================================
-- 初始化测试数据
-- =============================================

-- 插入品牌数据
INSERT INTO `brand` (`name`, `logo`, `description`, `sort`, `status`) VALUES
('Apple', 'https://example.com/brand/apple.png', '苹果公司，全球知名科技品牌', 1, 1),
('华为', 'https://example.com/brand/huawei.png', '华为技术有限公司，全球领先的ICT解决方案提供商', 2, 1),
('小米', 'https://example.com/brand/xiaomi.png', '小米科技有限责任公司，智能硬件和电子产品研发企业', 3, 1),
('Lenovo', 'https://example.com/brand/lenovo.png', '联想集团，全球PC领导企业', 4, 1),
('优衣库', 'https://example.com/brand/uniqlo.png', '日本快时尚品牌，提供高品质休闲服饰', 5, 1),
('ZARA', 'https://example.com/brand/zara.png', '西班牙快时尚品牌，全球知名时装零售商', 6, 1),
('三星', 'https://example.com/brand/samsung.png', '韩国三星集团，全球知名电子产品制造商', 7, 1),
('OPPO', 'https://example.com/brand/oppo.png', 'OPPO广东移动通信有限公司，知名手机品牌', 8, 1),
('vivo', 'https://example.com/brand/vivo.png', '维沃移动通信有限公司，知名手机品牌', 9, 1),
('荣耀', 'https://example.com/brand/honor.png', '荣耀终端有限公司，年轻科技潮牌', 10, 1);

-- 插入商品图片数据
INSERT INTO `product_image` (`product_id`, `image_url`, `image_type`, `sort`) VALUES
(1, 'https://example.com/product/iphone15_1.jpg', 1, 1),
(1, 'https://example.com/product/iphone15_2.jpg', 2, 1),
(1, 'https://example.com/product/iphone15_3.jpg', 2, 2),
(2, 'https://example.com/product/macbook_1.jpg', 1, 1),
(2, 'https://example.com/product/macbook_2.jpg', 2, 1),
(3, 'https://example.com/product/mate60_1.jpg', 1, 1),
(3, 'https://example.com/product/mate60_2.jpg', 2, 1),
(4, 'https://example.com/product/xiaomi14_1.jpg', 1, 1),
(4, 'https://example.com/product/xiaomi14_2.jpg', 2, 1),
(5, 'https://example.com/product/thinkpad_1.jpg', 1, 1),
(6, 'https://example.com/product/jacket_1.jpg', 1, 1),
(6, 'https://example.com/product/jacket_2.jpg', 2, 1),
(7, 'https://example.com/product/dress_1.jpg', 1, 1),
(7, 'https://example.com/product/dress_2.jpg', 2, 1);

-- 插入商品SKU数据
INSERT INTO `product_sku` (`product_id`, `sku_code`, `sku_name`, `spec_json`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
(1, 'IPHONE15PM-256-BLACK', 'iPhone 15 Pro Max 256GB 黑色', '{"颜色":"黑色","存储":"256GB"}', 9999.00, 10999.00, 50, 200, 1),
(1, 'IPHONE15PM-256-WHITE', 'iPhone 15 Pro Max 256GB 白色', '{"颜色":"白色","存储":"256GB"}', 9999.00, 10999.00, 30, 150, 1),
(1, 'IPHONE15PM-512-BLACK', 'iPhone 15 Pro Max 512GB 黑色', '{"颜色":"黑色","存储":"512GB"}', 11999.00, 12999.00, 20, 100, 1),
(2, 'MACBOOK14-M3-512', 'MacBook Pro 14 M3 512GB', '{"处理器":"M3","存储":"512GB"}', 14999.00, 15999.00, 30, 150, 1),
(2, 'MACBOOK14-M3-1TB', 'MacBook Pro 14 M3 1TB', '{"处理器":"M3","存储":"1TB"}', 17999.00, 18999.00, 20, 100, 1),
(3, 'MATE60PRO-256-BLACK', '华为Mate 60 Pro 256GB 黑色', '{"颜色":"黑色","存储":"256GB"}', 6999.00, 7999.00, 100, 400, 1),
(3, 'MATE60PRO-256-WHITE', '华为Mate 60 Pro 256GB 白色', '{"颜色":"白色","存储":"256GB"}', 6999.00, 7999.00, 80, 350, 1),
(4, 'XIAOMI14-256-BLACK', '小米14 Ultra 256GB 黑色', '{"颜色":"黑色","存储":"256GB"}', 5999.00, 6499.00, 80, 300, 1),
(4, 'XIAOMI14-512-WHITE', '小米14 Ultra 512GB 白色', '{"颜色":"白色","存储":"512GB"}', 6499.00, 6999.00, 70, 250, 1),
(6, 'JACKET-M-BLACK', '男士休闲夹克 M码 黑色', '{"颜色":"黑色","尺码":"M"}', 299.00, 399.00, 200, 400, 1),
(6, 'JACKET-L-BLACK', '男士休闲夹克 L码 黑色', '{"颜色":"黑色","尺码":"L"}', 299.00, 399.00, 150, 350, 1),
(6, 'JACKET-XL-BLACK', '男士休闲夹克 XL码 黑色', '{"颜色":"黑色","尺码":"XL"}', 299.00, 399.00, 150, 300, 1),
(7, 'DRESS-S-RED', '女士连衣裙 S码 红色', '{"颜色":"红色","尺码":"S"}', 459.00, 599.00, 100, 300, 1),
(7, 'DRESS-M-RED', '女士连衣裙 M码 红色', '{"颜色":"红色","尺码":"M"}', 459.00, 599.00, 100, 280, 1),
(7, 'DRESS-L-BLUE', '女士连衣裙 L码 蓝色', '{"颜色":"蓝色","尺码":"L"}', 459.00, 599.00, 100, 270, 1);

-- 插入商品参数数据
INSERT INTO `product_param` (`product_id`, `param_name`, `param_value`, `sort`) VALUES
(1, '屏幕尺寸', '6.7英寸', 1),
(1, '屏幕分辨率', '2796×1290像素', 2),
(1, '处理器', 'A17 Pro', 3),
(1, '运行内存', '8GB', 4),
(1, '后置摄像头', '4800万像素', 5),
(1, '前置摄像头', '1200万像素', 6),
(1, '电池容量', '4422mAh', 7),
(1, '操作系统', 'iOS 17', 8),
(2, '屏幕尺寸', '14.2英寸', 1),
(2, '屏幕分辨率', '3024×1964像素', 2),
(2, '处理器', 'Apple M3', 3),
(2, '运行内存', '8GB', 4),
(2, '存储容量', '512GB', 5),
(2, '续航时间', '约17小时', 6),
(3, '屏幕尺寸', '6.82英寸', 1),
(3, '屏幕分辨率', '2720×1260像素', 2),
(3, '处理器', '麒麟9000S', 3),
(3, '运行内存', '12GB', 4),
(3, '后置摄像头', '5000万像素', 5),
(3, '电池容量', '5000mAh', 6),
(4, '屏幕尺寸', '6.73英寸', 1),
(4, '屏幕分辨率', '3200×1440像素', 2),
(4, '处理器', '骁龙8 Gen 3', 3),
(4, '运行内存', '16GB', 4),
(4, '后置摄像头', '5000万像素', 5),
(4, '电池容量', '5000mAh', 6),
(6, '材质', '聚酯纤维', 1),
(6, '适用季节', '春秋', 2),
(6, '风格', '休闲', 3),
(6, '厚度', '常规', 4),
(7, '材质', '棉纶', 1),
(7, '适用季节', '夏季', 2),
(7, '风格', '时尚', 3),
(7, '裙长', '中长裙', 4);

-- 插入商品评价数据
INSERT INTO `product_comment` (`product_id`, `user_id`, `order_id`, `content`, `images`, `rating`, `is_anonymous`, `status`) VALUES
(1, 1, NULL, '手机非常好用，运行流畅，拍照效果很棒！', 'https://example.com/comment/1.jpg,https://example.com/comment/2.jpg', 5, 0, 1),
(1, 2, NULL, '外观设计很漂亮，手感很好，就是价格有点贵。', NULL, 4, 0, 1),
(2, 1, NULL, 'MacBook Pro性能强劲，屏幕显示效果出色，非常满意！', 'https://example.com/comment/3.jpg', 5, 0, 1),
(3, 2, NULL, '华为手机信号很好，拍照清晰，支持国货！', NULL, 5, 0, 1),
(4, 1, NULL, '小米手机性价比很高，配置强悍，推荐购买。', 'https://example.com/comment/4.jpg', 5, 0, 1),
(6, 2, NULL, '夹克质量不错，穿着很舒服，尺码标准。', NULL, 4, 0, 1),
(7, 1, NULL, '连衣裙款式很好看，面料舒适，老婆很喜欢！', 'https://example.com/comment/5.jpg', 5, 0, 1);

-- =============================================
-- 数据库表结构说明
-- =============================================
-- 1. 用户表：存储用户基本信息，支持用户名、手机号唯一登录
-- 2. 商品分类表：支持多级分类（最多三级），通过parent_id和level实现
-- 3. 商品表：存储商品基本信息，包含价格、库存、销量等
-- 4. 购物车表：记录用户购物车商品，同一用户同一商品只能有一条记录
-- 5. 订单表：记录订单基本信息，包含订单状态流转、收货信息等
-- 6. 订单详情表：记录订单中的商品明细，冗余商品信息防止商品修改影响历史订单
-- 7. 品牌表：存储品牌基本信息，包含品牌名称、Logo、描述等
-- 8. 商品图片表：存储商品多张图片，区分主图、详情图、规格图
-- 9. 商品SKU规格表：存储商品不同规格的库存、价格信息
-- 10. 商品参数表：存储商品详细参数信息
-- 11. 商品评价表：存储用户对商品的评价信息，支持图片、评分、商家回复

-- =============================================
-- 索引设计说明
-- =============================================
-- 1. 主键索引：所有表都有自增主键
-- 2. 唯一索引：用户名、手机号、订单号、品牌名称、SKU编码等唯一字段
-- 3. 普通索引：外键字段、查询频繁的字段（如status、create_time、rating等）
-- 4. 联合索引：购物车表的(user_id, product_id)保证唯一性
-- 5. 商品评价表索引：支持按商品、用户、订单、评分等多维度查询
-- 6. 商品SKU表索引：支持按商品ID快速查询所有SKU
-- 7. 商品图片表索引：支持按商品ID和图片类型查询

-- =============================================
-- 字段设计说明
-- =============================================
-- 1. 所有表都包含 create_time、update_time 字段，自动维护时间戳
-- 2. 主要表都包含 is_deleted 字段，实现逻辑删除
-- 3. 金额字段使用 DECIMAL(10, 2) 类型，避免精度丢失
-- 4. 状态字段使用 TINYINT 类型，节省存储空间
-- 5. 文本字段使用 TEXT 类型，支持大文本存储
-- 6. SKU规格表使用 JSON 类型存储规格属性，灵活支持不同商品的规格
-- 7. 商品图片表通过 image_type 区分主图、详情图、规格图
-- 8. 商品评价表支持匿名评价、商家回复、图片上传等功能
