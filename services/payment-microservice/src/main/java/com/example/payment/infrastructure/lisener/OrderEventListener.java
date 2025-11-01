package com.example.payment.infrastructure.lisener;




import com.example.payment.infrastructure.config.RabbitMQConfig;
import com.example.payment.infrastructure.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    // Giả sử bạn có 1 service để xử lý logic thanh toán
    // private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    // Lắng nghe tin nhắn từ hàng đợi PAYMENT_QUEUE
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handleOrderCreatedEvent(String messagePayload) {
        log.info("Received new message: {}", messagePayload);

        try {
            // 1. Convert chuỗi JSON ngược lại thành Object
            OrderCreatedEvent event = objectMapper.readValue(messagePayload, OrderCreatedEvent.class);

            log.info("Processing payment for orderId: {}, amount: {} {}",
                    event.getOrderId(), event.getGrandTotal(), event.getCurrency());

            // 2. TODO: GỌI LOGIC XỬ LÝ THANH TOÁN CỦA BẠN TẠI ĐÂY
            // paymentService.processPayment(event);

            // (Giả lập xử lý thành công)
            log.info("Payment successful for orderId: {}", event.getOrderId());

            // Sau khi thanh toán xong, payment-service cũng sẽ
            // tạo 1 OutboxEvent ("PAYMENT_SUCCESSFUL")
            // và gửi lại cho order-service để cập nhật trạng thái order

        } catch (JsonProcessingException e) {
            log.error("Failed to parse OrderCreatedEvent: {}", messagePayload, e);
            // Xử lý lỗi (ví dụ: đưa vào Dead Letter Queue)
        } catch (Exception e) {
            log.error("Failed to process payment for message: {}", messagePayload, e);
            // Xử lý lỗi (ví dụ: retry)
        }
    }
}