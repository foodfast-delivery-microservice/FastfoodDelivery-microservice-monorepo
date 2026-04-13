package com.example.notificationservice.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitMQConfig {

    // Payment exchange và routing keys
    public static final String PAYMENT_EXCHANGE = "payment_exchange";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";

    // Order exchange và routing keys
    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status.changed";

    // Queues cho notification service
    public static final String PAYMENT_SUCCESS_QUEUE = "notification.payment.success.queue";
    public static final String PAYMENT_FAILED_QUEUE = "notification.payment.failed.queue";
    public static final String PAYMENT_REFUNDED_QUEUE = "notification.payment.refunded.queue";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "notification.order.status.changed.queue";

    // Dead Letter Queues
    public static final String PAYMENT_SUCCESS_DLQ = "notification.payment.success.dlq";
    public static final String PAYMENT_FAILED_DLQ = "notification.payment.failed.dlq";
    public static final String PAYMENT_REFUNDED_DLQ = "notification.payment.refunded.dlq";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
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
    public Queue paymentSuccessDlq() {
        return new Queue(PAYMENT_SUCCESS_DLQ, true);
    }

    @Bean
    public Queue paymentFailedDlq() {
        return new Queue(PAYMENT_FAILED_DLQ, true);
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return new Queue(PAYMENT_REFUNDED_QUEUE, true);
    }

    @Bean
    public Queue paymentRefundedDlq() {
        return new Queue(PAYMENT_REFUNDED_DLQ, true);
    }

    @Bean
    public Queue orderStatusChangedQueue() {
        return new Queue(ORDER_STATUS_CHANGED_QUEUE, true);
    }

    @Bean
    public Binding bindingPaymentSuccess(Queue paymentSuccessQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentSuccessQueue)
                .to(paymentExchange)
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding bindingPaymentFailed(Queue paymentFailedQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentFailedQueue)
                .to(paymentExchange)
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingPaymentRefunded(Queue paymentRefundedQueue, TopicExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentRefundedQueue)
                .to(paymentExchange)
                .with(PAYMENT_REFUNDED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingOrderStatusChanged(Queue orderStatusChangedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderStatusChangedQueue)
                .to(orderExchange)
                .with(ORDER_STATUS_CHANGED_ROUTING_KEY);
    }

    // Generic Notification
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_DLQ = "notification.dlq";
    public static final String NOTIFICATION_ROUTING_KEY = "#";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return org.springframework.amqp.core.QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding bindingNotification(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding bindingNotificationDlq(Queue notificationDlq, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationDlq).to(notificationExchange).with("notification.dlq");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }
}
