package com.mall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUENE = "seckill.quene";
    public static final String SECKILL_ROUTING_KEY = "seckill.key";


    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay.key";

    public static final String ORDER_CANCEL_EXCHANGE = "order.cancel.exchange";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel.key";


    public static final String ORDER_CANCEL_RESOURCE_EXCHANGE = "order.cancel.resource.exchange";
    public static final String ORDER_CANCEL_RESOURCE_QUEUE = "order.cancel.resource.queue";
    public static final String ORDER_CANCEL_RESOURCE_ROUTING_KEY = "order.cancel.resource.key";

    public static final String PAYMENT_SUCCESS_EXCHANGE = "payment.success.exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success.key";


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
        return rabbitTemplate;
    }

    @Bean
    public Exchange orderDelayExchange() {
        return ExchangeBuilder.directExchange(ORDER_DELAY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_CANCEL_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY);
        return QueueBuilder.durable(ORDER_DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, Exchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderDelayExchange).with(ORDER_DELAY_ROUTING_KEY).noargs();
    }

    @Bean
    public Exchange orderCancelExchange() {
        return ExchangeBuilder.directExchange(ORDER_CANCEL_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE).build();
    }

    @Bean
    public Binding orderCancelBinding(Queue orderCancelQueue, Exchange orderCancelExchange) {
        return BindingBuilder.bind(orderCancelQueue).to(orderCancelExchange).with(ORDER_CANCEL_ROUTING_KEY).noargs();
    }

    @Bean
    public Exchange orderCancelResourceExchange() {
        return ExchangeBuilder.directExchange(ORDER_CANCEL_RESOURCE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderCancelResourceQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_RESOURCE_QUEUE).build();
    }

    @Bean
    public Binding orderCancelResourceBinding(Queue orderCancelResourceQueue, Exchange orderCancelResourceExchange) {
        return BindingBuilder.bind(orderCancelResourceQueue).to(orderCancelResourceExchange).with(ORDER_CANCEL_RESOURCE_ROUTING_KEY).noargs();
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
