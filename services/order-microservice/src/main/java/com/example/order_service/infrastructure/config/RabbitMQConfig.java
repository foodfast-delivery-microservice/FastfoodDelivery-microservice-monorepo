package com.example.order_service.infrastructure.config;

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
    public static final String PAYMENT_EXCHANGE = "payment_exchange";
    public static final String USER_EVENTS_EXCHANGE = "user.events";

    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";
    public static final String PAYMENT_REFUNDED_QUEUE = "payment.refunded.queue";

    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";

    public static final String ORDER_REFUNDED_QUEUE = "order.refunded.queue";
    public static final String ORDER_REFUNDED_ROUTING_KEY = "order.refunded";
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
    public static final String ORDER_MERCHANT_DEACTIVATED_QUEUE = "order.merchant.deactivated.queue";
    public static final String MERCHANT_DEACTIVATED_ROUTING_KEY = "merchant.deactivated";

    // Drone Service Exchange
    public static final String DRONE_EXCHANGE = "drone_exchange";
    public static final String DRONE_ASSIGNED_QUEUE = "order.drone.assigned.queue";
    public static final String DRONE_ASSIGNED_ROUTING_KEY = "drone.assigned";
    public static final String DELIVERY_COMPLETED_QUEUE = "order.delivery.completed.queue";
    public static final String DELIVERY_COMPLETED_ROUTING_KEY = "drone.delivery.completed";


    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange droneExchange() {
        return new TopicExchange(DRONE_EXCHANGE);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return new Queue(PAYMENT_REFUNDED_QUEUE, true);
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
    public Queue orderMerchantDeactivatedQueue() {
        return new Queue(ORDER_MERCHANT_DEACTIVATED_QUEUE, true);
    }

    @Bean
    public Queue droneAssignedQueue() {
        return new Queue(DRONE_ASSIGNED_QUEUE, true);
    }

    @Bean
    public Queue deliveryCompletedQueue() {
        return new Queue(DELIVERY_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentRefundedQueue())
                .to(paymentExchange())
                .with(PAYMENT_REFUNDED_ROUTING_KEY);
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
        return BindingBuilder.bind(orderMerchantDeactivatedQueue())
                .to(userEventsExchange())
                .with(MERCHANT_DEACTIVATED_ROUTING_KEY);
    }

    @Bean
    public Binding droneAssignedBinding() {
        return BindingBuilder.bind(droneAssignedQueue())
                .to(droneExchange())
                .with(DRONE_ASSIGNED_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryCompletedBinding() {
        return BindingBuilder.bind(deliveryCompletedQueue())
                .to(droneExchange())
                .with(DELIVERY_COMPLETED_ROUTING_KEY);
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