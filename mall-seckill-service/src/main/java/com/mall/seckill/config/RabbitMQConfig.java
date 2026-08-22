package com.mall.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // 交换机名称
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    // 队列名称
    public static final String SECKILL_QUEUE = "seckill.queue";
    // 路由键
    public static final String SECKILL_ROUTING_KEY = "seckill.key";


    // 声明交换机（Direct类型）
    @Bean
    public Exchange seckillExchange(){
        return ExchangeBuilder.directExchange(SECKILL_EXCHANGE).
                durable(true).build();
    }

    // 声明队列
    @Bean
    public Queue seckillQueue(){
        return QueueBuilder.durable(SECKILL_QUEUE).build();
    }

    // 绑定队列到交换机
    @Bean
    public Binding seckillBinding(Queue seckillQueue, Exchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue)
                .to(seckillExchange)
                .with(SECKILL_ROUTING_KEY)
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
        return rabbitTemplate;
    }
}

