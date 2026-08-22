package com.mall.payment.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

// 支付流水号生成
public class PaymentNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    /**
     * 生成支付流水号
     * 格式：PAY + 时间戳(yyyyMMddHHmmss) + 4位序列号
     * 例如：PAY202606091530450001
     */
    public static String generate() {
        String localTime = LocalDateTime.now().format(FORMATTER);
        long sequence = SEQUENCE.incrementAndGet() % 10000;
        return String.format("PAY%s%04d", localTime, sequence);
    }

}
