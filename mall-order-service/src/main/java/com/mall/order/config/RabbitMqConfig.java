package com.mall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitMqConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUENE = "seckill.quene";
    public static final String SECKILL_ROUTING_KEY = "seckill.key";


    // 注意：原 order.delay.exchange / order.delay.queue / order.cancel.exchange / order.cancel.queue
    // 整条延迟消息链路已移除。超时关单改由 OrderTimeoutTask 定时扫表驱动，
    // 理由见 OrderTimeoutTask 的类注释：orders 表才是超时的事实来源，
    // 延迟消息作为唯一依据会在"发消息前宕机"时造成订单永久卡单、库存永久占用。

    public static final String ORDER_CANCEL_RESOURCE_EXCHANGE = "order.cancel.resource.exchange";
    public static final String ORDER_CANCEL_RESOURCE_QUEUE = "order.cancel.resource.queue";
    public static final String ORDER_CANCEL_RESOURCE_ROUTING_KEY = "order.cancel.resource.key";

    public static final String PAYMENT_SUCCESS_EXCHANGE = "payment.success.exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success.key";

    /** 统一死信交换机：消费重试耗尽后，消息按「原队列名」路由到对应死信队列，便于人工排查 */
    public static final String MALL_DLX = "mall.dlx";
    public static final String ORDER_CANCEL_RESOURCE_DLQ = "order.cancel.resource.dlq";


    @Bean
    public Exchange seckillExchange() {
        return ExchangeBuilder.directExchange(SECKILL_EXCHANGE).
                durable(true).build();
    }

    @Bean
    public Queue seckillQuene() {
        return QueueBuilder.durable(SECKILL_QUENE).build();
    }

    @Bean
    public Binding seckillBinging(Queue seckillQuene, Exchange seckillExchange) {
        return BindingBuilder.bind(seckillQuene).to(seckillExchange).
                with(SECKILL_ROUTING_KEY).
                noargs();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        // 到了 Exchange 但路由不到任何队列时会被退回，默认静默丢弃；这里必须把它暴露出来
        rabbitTemplate.setReturnsCallback(returned ->
                log.error("消息无法路由到队列已被退回：exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        // ack=false 表示消息根本没进到 Exchange（Broker 挂了/网络抖动）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息未到达 Broker，需要补偿重发：correlationData={}, cause={}",
                        correlationData == null ? null : correlationData.getId(), cause);
            }
        });
        return rabbitTemplate;
    }

    @Bean
    public Exchange orderCancelResourceExchange() {
        return ExchangeBuilder.directExchange(ORDER_CANCEL_RESOURCE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderCancelResourceQueue() {
        // 回补库存失败同样进死信，避免毒消息在业务队列里反复投递
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MALL_DLX);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_RESOURCE_QUEUE);
        return QueueBuilder.durable(ORDER_CANCEL_RESOURCE_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding orderCancelResourceBinding(Queue orderCancelResourceQueue, Exchange orderCancelResourceExchange) {
        return BindingBuilder.bind(orderCancelResourceQueue).to(orderCancelResourceExchange).with(ORDER_CANCEL_RESOURCE_ROUTING_KEY).noargs();
    }

    // ============ 死信：消费失败的消息落到这里，人工排查/补偿 job 从这里捞 ============

    @Bean
    public Exchange mallDeadLetterExchange() {
        return ExchangeBuilder.directExchange(MALL_DLX).durable(true).build();
    }

    @Bean
    public Queue orderCancelResourceDlq() {
        return QueueBuilder.durable(ORDER_CANCEL_RESOURCE_DLQ).build();
    }

    @Bean
    public Binding orderCancelResourceDlqBinding(Queue orderCancelResourceDlq, Exchange mallDeadLetterExchange) {
        return BindingBuilder.bind(orderCancelResourceDlq).to(mallDeadLetterExchange).with(ORDER_CANCEL_RESOURCE_QUEUE).noargs();
    }

    @Bean
    public Exchange paymentSuccessExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_SUCCESS_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, Exchange paymentSuccessExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(paymentSuccessExchange).with(PAYMENT_SUCCESS_ROUTING_KEY).noargs();
    }
}
