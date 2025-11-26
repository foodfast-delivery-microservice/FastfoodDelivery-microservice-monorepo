package com.example.demo.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String ORDER_REFUNDED_QUEUE = "order.refunded.queue";
    public static final String ORDER_REFUNDED_ROUTING_KEY = "order.refunded";
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
    public static final String USER_EVENTS_EXCHANGE = "user.events";
    public static final String MERCHANT_DEACTIVATED_QUEUE = "product.merchant.deactivated.queue";
    public static final String MERCHANT_DEACTIVATED_ROUTING_KEY = "merchant.deactivated";
    public static final String MERCHANT_ACTIVATED_QUEUE = "product.merchant.activated.queue";
    public static final String MERCHANT_ACTIVATED_ROUTING_KEY = "merchant.activated";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderRefundedQueue() {
        return new Queue(ORDER_REFUNDED_QUEUE, true);
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue(ORDER_PAID_QUEUE, true);
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue productMerchantDeactivatedQueue() {
        return new Queue(MERCHANT_DEACTIVATED_QUEUE, true);
    }

    @Bean
    public Queue productMerchantActivatedQueue() {
        return new Queue(MERCHANT_ACTIVATED_QUEUE, true);
    }

    @Bean
    public Binding orderRefundedBinding() {
        return BindingBuilder.bind(orderRefundedQueue())
                .to(orderExchange())
                .with(ORDER_REFUNDED_ROUTING_KEY);
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue())
                .to(orderExchange())
                .with(ORDER_PAID_ROUTING_KEY);
    }

    @Bean
    public Binding merchantDeactivatedBinding() {
        return BindingBuilder.bind(productMerchantDeactivatedQueue())
                .to(userEventsExchange())
                .with(MERCHANT_DEACTIVATED_ROUTING_KEY);
    }

    @Bean
    public Binding merchantActivatedBinding() {
        return BindingBuilder.bind(productMerchantActivatedQueue())
                .to(userEventsExchange())
                .with(MERCHANT_ACTIVATED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }
}

