package com.mall.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 * 启动时自动创建所需的数据库表
 */
@Slf4j
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 删除已存在的 payment 表（确保表结构正确）
            jdbcTemplate.execute("DROP TABLE IF EXISTS payment");
            log.info("已删除旧的 payment 表（如果存在）");
            
            // 创建 payment 表
            String createPaymentTable = """
                CREATE TABLE payment (
                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付ID',
                    payment_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
                    order_id BIGINT DEFAULT NULL COMMENT '订单ID',
                    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
                    payment_type TINYINT DEFAULT 1 COMMENT '支付方式：1-微信，2-支付宝，3-银行卡',
                    status TINYINT DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付成功，2-支付失败，3-已退款',
                    transaction_id VARCHAR(100) DEFAULT NULL COMMENT '第三方支付流水号',
                    payment_time DATETIME DEFAULT NULL COMMENT '支付时间',
                    refund_time DATETIME DEFAULT NULL COMMENT '退款时间',
                    note VARCHAR(500) DEFAULT NULL COMMENT '备注',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_payment_no (payment_no),
                    KEY idx_order_id (order_id),
                    KEY idx_order_no (order_no),
                    KEY idx_user_id (user_id),
                    KEY idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表'
                """;
            
            jdbcTemplate.execute(createPaymentTable);
            log.info("payment 表创建成功");

            // 删除已存在的 payment_callback 表
            jdbcTemplate.execute("DROP TABLE IF EXISTS payment_callback");
            log.info("已删除旧的 payment_callback 表（如果存在）");
            
            // 创建 payment_callback 表
            String createCallbackTable = """
                CREATE TABLE payment_callback (
                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '回调ID',
                    payment_id BIGINT NOT NULL COMMENT '支付ID',
                    payment_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
                    callback_status TINYINT DEFAULT 0 COMMENT '回调状态：0-失败，1-成功',
                    callback_data TEXT DEFAULT NULL COMMENT '回调数据（JSON格式）',
                    callback_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                    PRIMARY KEY (id),
                    KEY idx_payment_id (payment_id),
                    KEY idx_payment_no (payment_no),
                    KEY idx_callback_status (callback_status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付回调记录表'
                """;
            
            jdbcTemplate.execute(createCallbackTable);
            log.info("payment_callback 表创建成功");

        } catch (Exception e) {
            log.error("数据库表初始化失败", e);
        }
    }
}
