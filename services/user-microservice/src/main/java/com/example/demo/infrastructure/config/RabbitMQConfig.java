package com.example.demo.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String USER_EVENTS_EXCHANGE = "user.events";

    // Restaurant deletion validation queues
    public static final String RESTAURANT_VALIDATION_PASSED_QUEUE = "user.restaurant.validation.passed.queue";
    public static final String RESTAURANT_VALIDATION_FAILED_QUEUE = "user.restaurant.validation.failed.queue";
    public static final String RESTAURANT_VALIDATION_PASSED_ROUTING_KEY = "restaurant.validation.passed";
    public static final String RESTAURANT_VALIDATION_FAILED_ROUTING_KEY = "restaurant.validation.failed";

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue restaurantValidationPassedQueue() {
        return new Queue(RESTAURANT_VALIDATION_PASSED_QUEUE, true);
    }

    @Bean
    public Queue restaurantValidationFailedQueue() {
        return new Queue(RESTAURANT_VALIDATION_FAILED_QUEUE, true);
    }

    @Bean
    public Binding restaurantValidationPassedBinding() {
        return BindingBuilder.bind(restaurantValidationPassedQueue())
                .to(userEventsExchange())
                .with(RESTAURANT_VALIDATION_PASSED_ROUTING_KEY);
    }

    @Bean
    public Binding restaurantValidationFailedBinding() {
        return BindingBuilder.bind(restaurantValidationFailedQueue())
                .to(userEventsExchange())
                .with(RESTAURANT_VALIDATION_FAILED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
