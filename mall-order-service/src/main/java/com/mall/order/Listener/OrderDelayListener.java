package com.mall.order.Listener;

import com.mall.common.Result;
import com.mall.order.entity.Item.OrderItem;
import com.mall.order.entity.Order;
import com.mall.order.feign.ProductServiceFeignClient;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderDelayListener {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductServiceFeignClient productServiceFeignClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 超时取消幂等键前缀 */
    private static final String ORDER_TIMEOUT_IDEMPOTENT_PREFIX = "order:timeout:idempotent:";

    /** 资源恢复（还库存）幂等键前缀 */
    private static final String ORDER_CANCEL_RESOURCE_IDEMPOTENT_PREFIX = "order:cancel:resource:";

    /**
     * 超时订单自动取消
     */
    @Transactional
    @RabbitListener(queues = "order.cancel.queue")
    public void handleOrderDelay(Long orderId) {
        // 幂等标志：同一订单的超时消息只处理一次，防止重复消费导致库存重复恢复
        String idempotentKey = ORDER_TIMEOUT_IDEMPOTENT_PREFIX + orderId;
        try {
            Boolean firstConsume = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(firstConsume)) {
                log.info("订单超时消息已处理，跳过重复消费，订单ID: {}", orderId);
                return;
            }

            Order order = orderMapper.selectById(orderId);

            if (order == null) {
                log.warn("订单不存在，订单ID: {}", orderId);
                return;
            }

            if (order.getStatus() == 0) {
                order.setStatus(4);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("订单超时自动取消成功，订单ID: {}", orderId);

                // 补发资源队列消息，异步恢复库存（事务提交后再发送）
                List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
                if (orderItems != null && !orderItems.isEmpty()) {
                    sendOrderCancelResourceMessageAfterCommit(orderId, orderItems);
                } else {
                    log.warn("订单无商品项，跳过资源恢复，订单ID: {}", orderId);
                }
            } else {
                log.info("订单已支付，无需取消，订单ID: {}", orderId);
                log.info("订单状态: {}", order.getStatus());
            }

        } catch (Exception e) {
            // 处理失败：释放幂等标志并向外抛出。@Transactional 会回滚订单状态，
            // 消息进入重试；重试耗尽后因队列已挂 DLX 且 default-requeue-rejected=false，落入死信队列
            redisTemplate.delete(idempotentKey);
            log.error("处理订单超时失败，订单ID: {}, 错误: {}", orderId, e.getMessage());
            throw new RuntimeException("处理订单超时失败，订单ID: " + orderId, e);
        }
    }

    /**
     * 发送资源恢复消息到资源队列，由 handleOrderCancel 消费并恢复库存
     */
    private void sendOrderCancelResourceMessage(Long orderId, List<OrderItem> orderItems) {
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
        log.info("订单资源恢复消息已补发，订单ID: {}", orderId);
    }

    /**
     * 在事务提交成功后再发送资源恢复消息。
     * 保证“订单取消落库”与“资源消息发出”的顺序：避免事务回滚但消息已发出的幽灵消息，
     * 也避免消费者先于提交读到旧状态；若当前无事务则直接发送。
     * 注意：afterCommit 阶段事务已提交，此处发送失败只能记日志，需要补偿机制（如本地消息表）兜底。
     */
    private void sendOrderCancelResourceMessageAfterCommit(Long orderId, List<OrderItem> orderItems) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendOrderCancelResourceMessage(orderId, orderItems);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sendOrderCancelResourceMessage(orderId, orderItems);
                } catch (Exception ex) {
                    log.error("事务提交后发送资源恢复消息失败，订单ID: {}，错误: {}", orderId, ex.getMessage());
                }
            }
        });
    }

    /**
     * 支付成功监听器
     */
    @Transactional
    @RabbitListener(queues = "payment.success.queue")
    public void handlePaymentSuccess(Map<String, Object> message){
        try{
//            提取消息内容
            Long orderId = Long.valueOf(message.get("orderId").toString());
            Long userId = Long.valueOf(message.get("userId").toString());
            String orderNo = message.get("orderNo").toString();
//            打印日志
            log.info("收到支付成功消息，订单ID: {}, 用户ID: {}", orderId, userId);
            
//            查询订单
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.warn("订单不存在，订单ID: {}", orderId);
                return;
            }
            
//            查询订单项
            List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
//            遍历并增加销量
           if(orderItems!=null&&!orderItems.isEmpty()){
               for(OrderItem item:orderItems){
                   productServiceFeignClient.increaseSales(
                           item.getProductId(),
                           item.getQuantity()
                   );
                   log.info("商品销量已增加，商品ID: {}, 数量: {}", item.getProductId(), item.getQuantity());
               }
           }
//            打印完成日志
            log.info("支付成功后续处理完成，订单ID: {}", orderId);
        } catch (Exception e) {
            log.error("支付成功后续处理失败,错误信息：{}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("支付成功后续处理失败", e);
        }
    }

    @RabbitListener(queues = "order.cancel.resource.queue")
    public void handleOrderCancel(Map<String, Object> message) {
        // 先提取 orderId，用于状态校验和幂等键
        Long orderId = Long.valueOf(message.get("orderId").toString());
        try {
            // 状态校验：只有确实处于“已取消”状态的订单才恢复库存，避免非法/过早消息把库存加错
            Order order = orderMapper.selectById(orderId);
            if (order == null || order.getStatus() == null || order.getStatus() != 4) {
                log.warn("订单不存在或未处于已取消状态，跳过库存恢复，订单ID: {}, 状态: {}",
                        orderId, order == null ? null : order.getStatus());
                return;
            }

            // 第1步：从消息中提取 orderItems
            List<Map<String, Object>> orderitem = (List<Map<String, Object>>) message.get("orderitem");
            // 第2步：打印日志，确认收到消息
            log.info("收到订单取消消息，订单ID: {}, 订单项数量: {}", orderId, orderitem.size());

            // 第3步：逐条回补库存。幂等键粒度是「订单+商品」而不是整条消息：
            // 中途某一项失败时整条消息会被重投，已成功的项必须跳过，否则会重复加库存
            for (Map<String, Object> item : orderitem) {
                Long productId = Long.valueOf(item.get("productId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());
                String itemKey = ORDER_CANCEL_RESOURCE_IDEMPOTENT_PREFIX + orderId + ":" + productId;

                Boolean firstRestore = redisTemplate.opsForValue()
                        .setIfAbsent(itemKey, "1", 24, TimeUnit.HOURS);
                if (Boolean.FALSE.equals(firstRestore)) {
                    log.info("该商品库存已回补过，跳过，订单ID: {}, 商品ID: {}", orderId, productId);
                    continue;
                }

                try {
                    Result<String> result = productServiceFeignClient.increaseStock(productId, quantity);
                    // 商品服务业务失败时 HTTP 仍返回 200，只有 Result.code 能识别；
                    // 不检查就会被当成消费成功、消息被 ACK，库存永久不回补
                    if (result == null || result.getCode() == null || result.getCode() != 200) {
                        throw new IllegalStateException("回补库存失败，商品ID: " + productId + "，原因: "
                                + (result == null ? "服务无响应" : result.getMessage()));
                    }
                    log.info("库存已恢复，商品ID: {}, 数量: {}", productId, quantity);
                } catch (RuntimeException ex) {
                    // 失败必须释放这一项的幂等键，否则重投时它会被永久跳过
                    redisTemplate.delete(itemKey);
                    throw ex;
                }
            }
            // 第4步：打印处理完成的日志
            log.info("订单取消处理完成，订单ID: {}", orderId);

        } catch (Exception e) {
            // 处理失败：向外抛出，让 Spring AMQP 判定消费失败。
            // 队列已挂 DLX 且 default-requeue-rejected=false，重试耗尽后消息进死信队列，不会无限重投
            log.error("处理订单取消消息失败，订单ID: {}, 错误: {}", orderId, e.getMessage());
            throw new RuntimeException("处理订单取消消息失败，订单ID: " + orderId, e);
        }
    }

}
