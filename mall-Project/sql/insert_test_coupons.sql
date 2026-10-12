-- ============================================
-- 优惠券测试数据 - 完整测试场景
-- ============================================

-- 步骤1：清空现有优惠券数据（谨慎使用）
-- DELETE FROM user_coupon WHERE coupon_id IN (SELECT id FROM coupon);
-- DELETE FROM coupon;

-- 步骤2：插入8种不同类型的测试优惠券
INSERT INTO coupon (
    name,           -- 优惠券名称
    type,           -- 类型：1=满减, 2=折扣, 3=无门槛
    value,          -- 面值：满减/无门槛是金额，折扣是折数（如8表示8折）
    min_amount,     -- 最低消费金额
    total_stock,    -- 总库存
    available_stock,-- 剩余库存
    start_time,     -- 开始时间
    end_time,       -- 结束时间
    status,         -- 上架状态：0=未上架, 1=已上架, 2=已停用（未开始/进行中/已过期由时间与库存派生）
    create_time     -- 创建时间
) VALUES
-- 【可抢购】满减券 - 新用户专享
('🎁 新用户专享券', 1, 50.00, 200.00, 100, 85, 
 NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, NOW()),

-- 【可抢购】满减券 - 大额优惠
('💰 全场满减券', 1, 100.00, 500.00, 50, 50, 
 NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, NOW()),

-- 【可抢购】折扣券 - 8折优惠
('🎯 限时8折券', 2, 8.00, 100.00, 200, 180, 
 NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY), 1, NOW()),

-- 【可抢购】无门槛券 - 新人福利
('🎉 新人无门槛券', 3, 20.00, 0.00, 300, 250, 
 NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), 1, NOW()),

-- 【未开始】已上架但未到开始时间（status=1，未开始由 start_time 派生）
('⏰ 明日秒杀券', 1, 80.00, 300.00, 100, 100, 
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 1, NOW()),

-- 【已过期】已上架但已过结束时间（status=1，过期由 end_time 派生）
('❌ 历史优惠券', 1, 30.00, 150.00, 100, 0, 
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 1, NOW()),

-- 【已抢光】库存为0
('🔥 超值抢购券', 1, 60.00, 250.00, 20, 0, 
 NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY), 1, NOW()),

-- 【可抢购】7折特惠
('⭐ 7折特惠券', 2, 7.00, 200.00, 150, 120, 
 NOW(), DATE_ADD(NOW(), INTERVAL 6 DAY), 1, NOW());

-- 步骤3：查看插入结果
SELECT 
    id AS 'ID',
    name AS '优惠券名称',
    CASE type 
        WHEN 1 THEN '满减'
        WHEN 2 THEN '折扣'
        WHEN 3 THEN '无门槛'
    END AS '类型',
    CASE type
        WHEN 1 THEN CONCAT('￥', value)
        WHEN 2 THEN CONCAT(value, '折')
        WHEN 3 THEN CONCAT('￥', value)
    END AS '面值',
    CASE 
        WHEN min_amount = 0 THEN '无门槛'
        ELSE CONCAT('满￥', min_amount)
    END AS '使用条件',
    CONCAT(available_stock, '/', total_stock) AS '库存',
    CASE status
        WHEN 0 THEN '未上架'
        WHEN 1 THEN '已上架'
        WHEN 2 THEN '已停用'
    END AS '上架状态',
    DATE_FORMAT(start_time, '%m-%d %H:%i') AS '开始时间',
    DATE_FORMAT(end_time, '%m-%d %H:%i') AS '结束时间'
FROM coupon 
ORDER BY id DESC;

-- 步骤4：测试提示
SELECT '✅ 测试数据插入完成！' AS '提示',
       '请访问 http://localhost:3000/coupon 查看优惠券页面' AS '操作';
