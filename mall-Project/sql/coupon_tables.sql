-- =============================================
-- 优惠券模块表结构 (coupon / user_coupon / seckill_log)
-- 数据库：MySQL 8.0+
-- 涉及服务：mall-coupon-service、mall-seckill-service、mall-project-admin
-- 创建时间：2026-09-26
--
-- 为什么单独一个脚本：
--   mall_database.sql 里只有 user/category/product/cart/orders/... 这些表，
--   没有优惠券模块的三张表。不执行本脚本的话，优惠券列表、抢券、管理端优惠券
--   管理都会直接报 "Table 'mall.coupon' doesn't exist"。
--
-- 执行顺序：
--   1. mall_database.sql            （基础表）
--   2. coupon_tables.sql            （本脚本，优惠券三张表）
--   3. insert_test_coupons.sql      （可选，8 条测试数据）
--
-- 命令行执行（Windows 控制台默认 gbk，一定要显式指定客户端字符集，否则脚本里的中文
-- 别名/注释会报 "Cannot convert string ... from gbk to utf8mb3"）：
--   mysql --default-character-set=utf8mb4 -u root -p -D mall < coupon_tables.sql
-- 用 Navicat / DataGrip 等图形客户端打开执行，或直接粘贴进客户端也可以。
--
-- 安全说明：
--   * 全部使用 CREATE TABLE IF NOT EXISTS，不 DROP、不覆盖已有数据，可反复执行；
--     已存在同名表时只会输出一条 warning，不会改动它的结构。
--   * 本脚本只负责「建全」，不负责「改老表」。老库若有缺列，见文件末尾的差异说明。
-- =============================================

USE mall;

-- =============================================
-- 1. 优惠券表 (coupon)
--
-- status 只是「上架状态」：0-未上架 1-已上架 2-已停用（终态，不能再上架）
-- 「未开始 / 进行中 / 已抢光 / 已过期」不落库，由
-- com.mall.common.CouponStatus 按 start_time / end_time / available_stock 派生，
-- 管理端和 C 端都读同一份派生结果，避免前后端各写一套状态判断。
-- =============================================
CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `name` VARCHAR(100) NOT NULL COMMENT '优惠券名称（管理端按名称查重）',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1-满减 2-折扣 3-无门槛',
    `value` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '面额：满减/无门槛为金额，折扣为折数（8=8折）',
    `min_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛：满多少元可用，0-无门槛',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `available_stock` INT NOT NULL DEFAULT 0 COMMENT '剩余库存',
    `limit_per_user` INT NOT NULL DEFAULT 1 COMMENT '每人限领数量（>=1）',
    `start_time` DATETIME NOT NULL COMMENT '开始时间（未到该时间派生为「未开始」）',
    `end_time` DATETIME NOT NULL COMMENT '结束时间（超过该时间派生为「已过期」）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '上架状态：0-未上架 1-已上架 2-已停用',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_status_end_time` (`status`, `end_time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- =============================================
-- 2. 用户优惠券表 (user_coupon)
--
-- obtain_time 必须带 DEFAULT CURRENT_TIMESTAMP：
--   两个抢券实现（mall-coupon-service 的同步版、mall-seckill-service 的 MQ 版）
--   都只 set userId / couponId / status / expireTime，没有 set obtainTime，
--   服务里也没有 MetaObjectHandler 兜底，少了默认值插入就会失败。
--
-- 故意不加 (user_id, coupon_id) 唯一键：
--   每人限领是靠 coupon.limit_per_user 配合
--   countByUserIdAndCouponId 计数实现的，limitPerUser 允许 1~99（管理端表单校验），
--   同一个用户合法地持有多张同一活动的券。加了唯一键会让 limitPerUser >= 2 时
--   第二次领取直接抛 DuplicateKeyException（500）。
-- =============================================
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '使用时的订单ID（未使用为 NULL）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未使用 1-已使用 2-已过期',
    `obtain_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间（代码不显式赋值，依赖默认值）',
    `use_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间（两处抢券都 set 了 now+7 天）',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_coupon_status` (`coupon_id`, `status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- =============================================
-- 3. 秒杀日志表 (seckill_log)
--
-- 列与 mall-seckill-service / mall-coupon-service 的 SeckillLog 实体一一对应。
-- order_id / fail_reason / coupon_name / coupon_amount 目前代码没赋值，
-- 但实体里有这几个字段，留着表结构才与实体一致（MyBatis-Plus 默认不插入 null 字段）。
--
-- 故意不加 (coupon_id, user_id) 唯一键：理由同 user_coupon，
--   limitPerUser >= 2 时同一个用户会对同一活动产生多条成功日志。
--
-- seckill_time / create_time / update_time 的位置与
-- mall-Project/sql/fix_seckill_log_table.sql 的补丁保持一致（seckill_time 在 create_time 前），
-- 这样「新库直接建全」和「老库跑补丁」得到的表结构相同。
-- =============================================
CREATE TABLE IF NOT EXISTS `seckill_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '订单ID（暂未使用）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '秒杀状态：0-失败 1-成功 2-待处理',
    `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因（暂未使用）',
    `coupon_name` VARCHAR(100) DEFAULT NULL COMMENT '优惠券名称（冗余，暂未使用）',
    `coupon_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '优惠券面额（暂未使用）',
    `seckill_time` DATETIME NULL COMMENT '秒杀时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_coupon` (`user_id`, `coupon_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀日志表';

-- =============================================
-- 验证：三张表是否建好，以及 user_coupon.obtain_time 是否带默认值
-- =============================================
SELECT TABLE_NAME AS '表名', TABLE_COMMENT AS '说明'
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('coupon', 'user_coupon', 'seckill_log');

SELECT COLUMN_NAME AS '字段', COLUMN_TYPE AS '类型', IS_NULLABLE AS '可空', COLUMN_DEFAULT AS '默认值'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_coupon'
  AND COLUMN_NAME IN ('status', 'obtain_time', 'use_time', 'expire_time');

SELECT '✅ 优惠券三张表检查完成' AS '提示',
       '如需测试数据，继续执行 insert_test_coupons.sql' AS '下一步';


-- =============================================
-- 附：与「老库」的差异说明（都是注释，不会执行，需要时再手工放开）
-- =============================================
--
-- A. seckill_log 老表可能缺 4 个字段（实体里有、表里没有）：
--    目前代码不给它们赋值，所以不补也能跑；补上表结构与实体才一致。
--    注意老表已有 seckill_time / create_time / update_time 时不要重复加。
--
-- ALTER TABLE `seckill_log`
--     ADD COLUMN `order_id` BIGINT DEFAULT NULL COMMENT '订单ID（暂未使用）',
--     ADD COLUMN `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因（暂未使用）',
--     ADD COLUMN `coupon_name` VARCHAR(100) DEFAULT NULL COMMENT '优惠券名称（暂未使用）',
--     ADD COLUMN `coupon_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '优惠券面额（暂未使用）';
--
-- B. 老库若为了「每人限领 1 张」加了下面两个唯一键，会让 limitPerUser >= 2 时
--    第二次领取抛 DuplicateKeyException。要支持每人限领多张，先确认没有重复数据再删：
--
--    限领的正确拦截点是「分布式锁内的 countByUserIdAndCouponId」，锁在事务提交后才释放；
--    MQ 异步路径的重复投递去重靠消费端的 Redis 键 seckill:msg:{messageId}（setIfAbsent，24h），
--    所以这里不需要、也不能用唯一键来做幂等。
--
-- SELECT user_id, coupon_id, COUNT(*) FROM `user_coupon` GROUP BY user_id, coupon_id HAVING COUNT(*) > 1;
-- SELECT user_id, coupon_id, COUNT(*) FROM `seckill_log` GROUP BY user_id, coupon_id HAVING COUNT(*) > 1;
--
-- ALTER TABLE `user_coupon` DROP INDEX `uk_user_coupon`;
-- ALTER TABLE `seckill_log`  DROP INDEX `uk_coupon_user`;
--
-- C. 老库 coupon 表若无 uk_name，管理端「名称查重」在并发下可能出现同名券：
-- ALTER TABLE `coupon` ADD UNIQUE KEY `uk_name` (`name`);