package com.example.payment.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;


@Configuration
public class RabbitMQConfig {

    // --- CẤU HÌNH NHẬN EVENT TỪ ORDER-SERVICE ---
    public static final String ORDER_EXCHANGE = "order_exchange";
    public static final String USER_EVENTS_EXCHANGE = "user.events";

    // tạo đơn hàng
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String PAYMENT_QUEUE = "payment.order_created.queue";

    // refund
    public static final String REFUND_REQUEST_QUEUE = "order.refund.request.queue";
    public static final String REFUND_REQUEST_ROUTING_KEY = "order.refund.request";

    @Bean
    public Queue refundRequestQueue() {
        // 1. TẠO RA CÁI QUEUE ĐANG BỊ BÁO LỖI 404
        return new Queue(REFUND_REQUEST_QUEUE, true);
    }

    @Bean
    public Binding bindingRefundRequest(Queue refundRequestQueue, TopicExchange orderExchange) {
        // 2. KẾT NỐI QUEUE ĐÓ VÀO CÙNG 'orderExchange'
        return BindingBuilder
                .bind(refundRequestQueue) // Nguồn: queue refund
                .to(orderExchange)         // Đích: exchange của order
                .with(REFUND_REQUEST_ROUTING_KEY); // Bằng khóa refund
    }
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue paymentMerchantDeactivatedQueue() {
        return new Queue(PAYMENT_MERCHANT_DEACTIVATED_QUEUE, true);
    }

    @Bean
    public Binding bindingOrderCreated(Queue paymentQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(orderExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding merchantDeactivatedBinding() {
        return BindingBuilder
                .bind(paymentMerchantDeactivatedQueue())
                .to(userEventsExchange())
                .with(MERCHANT_DEACTIVATED_ROUTING_KEY);
    }

    // --- CẤU HÌNH GỬI EVENT PHẢN HỒI VỀ ORDER-SERVICE ---
    public static final String PAYMENT_EXCHANGE = "payment_exchange";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";
    public static final String PAYMENT_MERCHANT_DEACTIVATED_QUEUE = "payment.merchant.deactivated.queue";
    public static final String MERCHANT_DEACTIVATED_ROUTING_KEY = "merchant.deactivated";
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
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

    /**
     * Cấu hình listener container factory để deserialize JSON messages thành objects
     * Điều này cần thiết để @RabbitListener có thể tự động convert message thành PaymentRequest
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }
}
