package com.mall.seckill.config;

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
public class RabbitMQConfig {
    // 交换机名称
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    // 队列名称
    public static final String SECKILL_QUEUE = "seckill.queue";
    // 路由键
    public static final String SECKILL_ROUTING_KEY = "seckill.key";

    // 死信：消费重试耗尽后的消息落在这里，人工排查/补偿 job 从这里捞
    public static final String MALL_DLX = "mall.dlx";
    public static final String SECKILL_DLQ = "seckill.dlq";


    // 声明交换机（Direct类型）
    @Bean
    public Exchange seckillExchange(){
        return ExchangeBuilder.directExchange(SECKILL_EXCHANGE).
                durable(true).build();
    }

    // 声明队列
    @Bean
    public Queue seckillQueue(){
        // 注意：给已存在的队列补这个参数会报 PRECONDITION_FAILED，
        // 需要在管理界面先删掉 seckill.queue（确认无未消费消息）让应用重建
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MALL_DLX);
        args.put("x-dead-letter-routing-key", SECKILL_QUEUE);
        return QueueBuilder.durable(SECKILL_QUEUE).withArguments(args).build();
    }

    // 绑定队列到交换机
    @Bean
    public Binding seckillBinding(Queue seckillQueue, Exchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue)
                .to(seckillExchange)
                .with(SECKILL_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public Exchange mallDeadLetterExchange() {
        return ExchangeBuilder.directExchange(MALL_DLX).durable(true).build();
    }

    @Bean
    public Queue seckillDlq() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public Binding seckillDlqBinding(Queue seckillDlq, Exchange mallDeadLetterExchange) {
        return BindingBuilder.bind(seckillDlq)
                .to(mallDeadLetterExchange)
                .with(SECKILL_QUEUE)
                .noargs();
    }

    // 使用 JSON 消息转换器，解决反序列化安全问题
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        // 路由不到队列（交换机存在但没有匹配的 binding）时回调，默认会被静默丢弃
        rabbitTemplate.setReturnsCallback(returned ->
                log.error("秒杀消息无法路由，被退回: exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return rabbitTemplate;
    }
}

