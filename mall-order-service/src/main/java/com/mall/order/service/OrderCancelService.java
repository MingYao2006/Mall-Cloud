package com.mall.order.service;

import com.mall.order.entity.Item.OrderItem;
import com.mall.order.entity.Order;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超时关单 + 库存回补的唯一入口。
 * <p>
 * 扫表任务（{@link com.mall.order.task.OrderTimeoutTask}）和 MQ 延迟消费都收敛到这一个方法，
 * 保证两条路径用的是同一套原子状态流转和同一套幂等语义，不会出现"一条路径改了状态、
 * 另一条路径还按旧状态做判断"的竞争。
 */
@Slf4j
@Service
public class OrderCancelService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 把「待支付」订单原子地改成「已取消」，并在事务提交后发出库存回补消息。
     * <p>
     * 幂等由条件更新保证：只有真正把 status 从 0 改成 4 的那一次调用会返回 true，
     * 并发调用（扫表任务与延迟消息、多实例扫表）里只有一个能成功。
     * 这比 Redis SETNX 更可靠——SETNX 的 key 一旦在"抢到锁但还没改状态"之间丢失，
     * 该订单在 TTL 内会永久无法取消；条件更新没有这个失败模式。
     *
     * @return true 表示本次调用完成了取消；false 表示订单已支付、已取消或不存在
     */
    @Transactional
    public boolean cancelPendingOrder(Long orderId) {
        if (orderId == null) {
            return false;
        }
        if (orderMapper.cancelIfPending(orderId) != 1) {
            return false;
        }

        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("订单无商品项，跳过库存回补，订单ID: {}", orderId);
            return true;
        }

        // 事务提交后再发消息：避免"订单取消回滚了但回补消息已发出"，导致库存凭空多出来
        sendRestoreMessageAfterCommit(orderId, orderItems);
        log.info("订单超时自动取消成功，订单ID: {}", orderId);
        return true;
    }

    private void sendRestoreMessageAfterCommit(Long orderId, List<OrderItem> orderItems) {
        Runnable task = () -> sendRestoreMessage(orderId, orderItems);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("事务提交后发送库存回补消息失败，订单ID: {}，错误: {}", orderId, e.getMessage());
                }
            }
        });
    }

    private void sendRestoreMessage(Long orderId, List<OrderItem> orderItems) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderId", orderId);
        List<Map<String, Object>> orderItemList = new ArrayList<>();
        for (OrderItem item : orderItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", item.getProductId());
            itemMap.put("quantity", item.getQuantity());
            orderItemList.add(itemMap);
        }
        message.put("orderitem", orderItemList);
        rabbitTemplate.convertAndSend(
                "order.cancel.resource.exchange",
                "order.cancel.resource.key",
                message
        );
        log.info("库存回补消息已发送，订单ID: {}", orderId);
    }
}
