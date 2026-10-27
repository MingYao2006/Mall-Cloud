package com.mall.order.task;

import com.mall.order.mapper.OrderMapper;
import com.mall.order.service.OrderCancelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单的兜底关单任务。
 * <p>
 * 设计定位：<b>超时关单的事实来源是 orders 表，不是 MQ 消息。</b>
 * 延迟消息只是让系统"更早发现"超时的优化手段，一旦它成为唯一依据，
 * 就会出现"订单落库成功、发消息前进程宕机 → 订单永久卡在待支付、库存永久占用"。
 * <p>
 * 有了这个任务，丢消息的后果从"正确性问题"降级为"最多延迟一个扫描周期的体验问题"。
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    /** 与下单时的业务约定保持一致：超过 30 分钟未支付即视为超时 */
    private static final long TIMEOUT_MINUTES = 30;

    /** 单批处理条数，避免一次捞出过多订单长时间占用连接 */
    private static final int BATCH_SIZE = 200;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderCancelService orderCancelService;

    /**
     * 扫描并关单。fixedDelay 保证上一轮跑完才开始计时，不会自我堆叠。
     */
    @Scheduled(fixedDelayString = "${mall.order.timeout-scan-interval-ms:60000}",
            initialDelayString = "${mall.order.timeout-scan-initial-delay-ms:30000}")
    public void scanTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        long lastId = 0L;
        int cancelled = 0;

        while (true) {
            // 用 id 游标分页而不是 LIMIT offset：订单量增长后深分页会越翻越慢
            List<Long> ids = orderMapper.selectTimeoutIds(deadline, lastId, BATCH_SIZE);
            if (ids == null || ids.isEmpty()) {
                break;
            }
            for (Long id : ids) {
                lastId = Math.max(lastId, id);
                try {
                    // 单条失败不能中断整批：下个周期会重新扫到它
                    if (orderCancelService.cancelPendingOrder(id)) {
                        cancelled++;
                    }
                } catch (Exception e) {
                    log.error("超时关单失败，订单ID: {}", id, e);
                }
            }
            if (ids.size() < BATCH_SIZE) {
                break;
            }
        }

        if (cancelled > 0) {
            log.info("超时关单任务完成，本次取消订单数: {}", cancelled);
        }
    }
}
