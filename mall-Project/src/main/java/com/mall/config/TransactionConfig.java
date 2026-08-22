package com.mall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务配置类
 * 启用 Spring 事务管理
 */
@Configuration
@EnableTransactionManagement// 这个注解用于启用 @Transactional 注解的支持
public class TransactionConfig {
    // MyBatis-Spring-Boot-Starter 会自动配置 DataSourceTransactionManager
    // 这里只需要启用事务管理注解即可
}
