package com.mall.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 数据库中存储的密码
        String dbPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        
        // 测试密码
        String rawPassword = "123456";
        
        // 验证密码
        boolean matches = encoder.matches(rawPassword, dbPassword);
        
        System.out.println("========================================");
        System.out.println("密码验证测试");
        System.out.println("========================================");
        System.out.println("输入密码: " + rawPassword);
        System.out.println("数据库密码: " + dbPassword);
        System.out.println("验证结果: " + (matches ? "✓ 匹配成功" : "✗ 匹配失败"));
        System.out.println("========================================");
        
        // 生成新的密码
        String newEncoded = encoder.encode(rawPassword);
        System.out.println("新生成的密码: " + newEncoded);
        System.out.println("新密码验证: " + encoder.matches(rawPassword, newEncoded));
    }
}
