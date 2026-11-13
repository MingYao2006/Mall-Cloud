package com.mall.order.service;

import com.mall.order.entity.Item.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 库存回补消息的目标 */
    private static final String RESTORE_EXCHANGE = "order.cancel.resource.exchange";
    private static final String RESTORE_ROUTING_KEY = "order.cancel.resource.key";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OutboxService outboxService;

    /**
     * 把「待支付」订单原子地改成「已取消」，并在同一事务内登记库存回补消息。
     * <p>
     * 幂等由条件更新保证：只有真正把 status 从 0 改成 4 的那一次调用会返回 true，
     * 并发调用（扫表任务与延迟消息、多实例扫表）里只有一个能成功。
     * 这比 Redis SETNX 更可靠——SETNX 的 key 一旦在"抢到锁但还没改状态"之间丢失，
     * 该订单在 TTL 内会永久无法取消；条件更新没有这个失败模式。
     * <p>
     * 消息不再直接发给 MQ，而是写进 {@code mq_outbox}：与状态更新共享同一个本地事务，
     * 所以"订单已取消"和"回补消息待发送"是原子的。进程随后宕机也不会丢——
     * 重启后 {@link com.mall.order.task.OutboxSendTask} 会把待投递记录发出去。
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

        outboxService.enqueue(
                "order:" + orderId + ":cancel-resource",
                RESTORE_EXCHANGE,
                RESTORE_ROUTING_KEY,
                buildRestoreMessage(orderId, orderItems));

        log.info("订单超时自动取消成功，库存回补消息已入队，订单ID: {}", orderId);
        return true;
    }

    /**
     * 组装库存回补消息体。抽成独立方法是因为下单失败的回补路径也要复用同一结构。
     */
    public static Map<String, Object> buildRestoreMessage(Long orderId, List<OrderItem> orderItems) {
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
        return message;
    }
}
