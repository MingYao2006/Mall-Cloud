-- ============================================
-- 本地消息表（Transactional Outbox）
-- ============================================
-- 解决的问题：订单落库成功、但发送 RabbitMQ 消息前服务宕机（或发送异常被吞），
--               消息永久丢失，订单卡在待支付、库存永久占用。
--
-- 方案：把"发消息"降级成"同事务内写一行记录"。
--   1. 业务事务里同时 INSERT 订单 + INSERT mq_outbox(status=0)，二者原子提交；
--   2. 事务提交后由 OutboxSendTask 轮询 status=0 的记录并投递；
--   3. 收到 Broker 的生产者确认（publisher confirm）后置 status=1；
--   4. 发送失败则 retry_count+1 并按指数退避推迟 next_retry_time。
--
-- 崩溃点分析（三种都安全）：
--   · 提交前崩溃 → outbox 与订单一起回滚，不留残记录；
--   · 提交后、投递前崩溃 → INIT 记录还在，重启后投递任务会发出去；
--   · 投递后、置 SENT 前崩溃 → 消息被重发，由消费端幂等挡住。
--
-- 本脚本可反复执行。

CREATE TABLE IF NOT EXISTS `mq_outbox` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_key` VARCHAR(128) NOT NULL COMMENT '业务幂等键，如 order:12345',
    `exchange_name` VARCHAR(128) NOT NULL COMMENT '目标交换机',
    `routing_key` VARCHAR(128) NOT NULL COMMENT '路由键',
    `payload` TEXT NOT NULL COMMENT '消息体（JSON）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待投递 1-已投递 2-已失败（超重试上限，需人工介入）',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `next_retry_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次可投递时间（指数退避）',
    `last_error` VARCHAR(500) DEFAULT NULL COMMENT '最近一次失败原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    -- 同一业务事件只允许一条待投递记录：即使业务代码被重复触发，也不会重复发消息
    UNIQUE KEY `uk_biz_key_exchange` (`biz_key`, `exchange_name`),
    -- 投递任务的扫描索引：status 等值 + next_retry_time 范围
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地消息表（Outbox），保证业务落库与消息投递的最终一致';

SELECT '✅ mq_outbox 表已就绪！' AS message;
