package com.example.droneservice.infrastructure.config;

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

    // ==================== EXCHANGES ====================
    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String DRONE_EXCHANGE = "drone_exchange";

    // ==================== QUEUES ====================
    // Listen to Order Service
    public static final String ORDER_READY_QUEUE = "drone.order.ready.queue";

    // Publish to Order Service
    public static final String DRONE_ASSIGNED_QUEUE = "drone.assigned.queue";
    public static final String DRONE_STATUS_UPDATE_QUEUE = "drone.status.update.queue";
    public static final String DELIVERY_COMPLETED_QUEUE = "drone.delivery.completed.queue";

    // ==================== ROUTING KEYS ====================
    public static final String ORDER_READY_ROUTING_KEY = "order.ready.ship";
    public static final String DRONE_ASSIGNED_ROUTING_KEY = "drone.assigned";
    public static final String DRONE_STATUS_UPDATE_ROUTING_KEY = "drone.status.update";
    public static final String DELIVERY_COMPLETED_ROUTING_KEY = "drone.delivery.completed";

    // ==================== EXCHANGE BEANS ====================
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange droneExchange() {
        return new TopicExchange(DRONE_EXCHANGE);
    }

    // ==================== QUEUE BEANS ====================
    @Bean
    public Queue orderReadyQueue() {
        return new Queue(ORDER_READY_QUEUE, true);
    }

    @Bean
    public Queue droneAssignedQueue() {
        return new Queue(DRONE_ASSIGNED_QUEUE, true);
    }

    @Bean
    public Queue droneStatusUpdateQueue() {
        return new Queue(DRONE_STATUS_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue deliveryCompletedQueue() {
        return new Queue(DELIVERY_COMPLETED_QUEUE, true);
    }

    // ==================== BINDINGS ====================
    @Bean
    public Binding orderReadyBinding() {
        return BindingBuilder.bind(orderReadyQueue())
                .to(orderExchange())
                .with(ORDER_READY_ROUTING_KEY);
    }

    @Bean
    public Binding droneAssignedBinding() {
        return BindingBuilder.bind(droneAssignedQueue())
                .to(droneExchange())
                .with(DRONE_ASSIGNED_ROUTING_KEY);
    }

    @Bean
    public Binding droneStatusUpdateBinding() {
        return BindingBuilder.bind(droneStatusUpdateQueue())
                .to(droneExchange())
                .with(DRONE_STATUS_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryCompletedBinding() {
        return BindingBuilder.bind(deliveryCompletedQueue())
                .to(droneExchange())
                .with(DELIVERY_COMPLETED_ROUTING_KEY);
    }

    // ==================== MESSAGE CONVERTER ====================
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
