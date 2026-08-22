package com.mall.order.Listener;

import com.mall.order.entity.Item.OrderItem;
import com.mall.order.entity.Order;
import com.mall.order.feign.ProductServiceFeignClient;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderDelayListener {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductServiceFeignClient productServiceFeignClient;

    /**
     * 超时订单自动取消
     */
    @Transactional
    @RabbitListener(queues = "order.cancel.queue")
    public void handleOrderDelay(Long orderId) {
        try {
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
            } else {
                log.info("订单已支付，无需取消，订单ID: {}, 状态: {}", orderId, order.getStatus());
            }

        } catch (Exception e) {
            log.error("处理订单超时失败，订单ID: {}, 错误: {}", orderId, e.getMessage());
        }
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
        try {
            // 第1步：从消息中提取 orderId 和 orderItems
            Long orderId = Long.valueOf(message.get("orderId").toString());
            List<Map<String, Object>> orderitem = (List<Map<String, Object>>) message.get("orderitem");
            // 第2步：打印日志，确认收到消息
            log.info("收到订单取消消息，订单ID: {}, 订单项数量: {}", orderId, orderitem.size());
            // 第3步：遍历订单项，调用 Feign 接口恢复库存
            for(Map<String, Object> item:orderitem){
                Long productId = Long.valueOf(item.get("productId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                productServiceFeignClient.increaseStock(productId, quantity);
                log.info("库存已恢复，商品ID: {}, 数量: {}", productId, quantity);
            }
            // 提示：productServiceFeignClient.increaseStock(productId, quantity)

            // 第4步：打印处理完成的日志
            log.info("订单取消处理完成，订单ID: {}", orderId);

        } catch (Exception e) {
            log.error("处理订单取消消息失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

}
