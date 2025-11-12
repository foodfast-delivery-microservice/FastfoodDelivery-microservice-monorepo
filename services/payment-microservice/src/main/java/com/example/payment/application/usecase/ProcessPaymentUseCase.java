package com.example.payment.application.usecase;

import com.example.payment.domain.model.EventStatus;
import com.example.payment.domain.model.OutboxEvent;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.port.OrderServicePort;
import com.example.payment.domain.port.UserServicePort;
import com.example.payment.domain.repository.OutboxEventRepository;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.client.dto.OrderDetailResponse;
import com.example.payment.infrastructure.client.dto.UserValidationResponse;
import com.example.payment.infrastructure.event.PaymentFailedEventPayload;
import com.example.payment.infrastructure.event.PaymentSuccessEventPayload;

import com.example.payment.application.dto.PaymentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OrderServicePort orderServicePort;
    private final UserServicePort userServicePort;

    /**
     * Create payment with PENDING status when order is created
     * This method is called by OrderEventListener when order is created
     * No validation is performed - payment is just created
     */
    @Transactional
    public void createPayment(PaymentRequest request) {
        // Validate userId is not null (should come from order service event)
        if (request.getUserId() == null) {
            log.error("UserId is required but not provided in payment creation request for orderId: {}", request.getOrderId());
            throw new RuntimeException("UserId is required for payment creation");
        }

        log.info("Creating payment for orderId: {}, userId: {}", request.getOrderId(), request.getUserId());

        // Check if payment already exists
        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            log.warn("Payment for orderId: {} already exists. Skipping creation.", request.getOrderId());
            return;
        }

        // Create payment with PENDING status
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getGrandTotal())
                .currency(request.getCurrency())
                .status(Payment.Status.PENDING)
                .build();

        paymentRepository.save(payment);
        log.info("Payment created with PENDING status for orderId: {}", request.getOrderId());
    }

    /**
     * Process payment - validate and execute payment
     * This method is called by PaymentController when user wants to pay
     * Only processes payment when order status is CONFIRMED
     */
    @Transactional
    public boolean execute(PaymentRequest request) {
        // Validate userId is not null (should be set from JWT token in PaymentController)
        if (request.getUserId() == null) {
            log.error("UserId is required but not provided in payment request for orderId: {}", request.getOrderId());
            throw new RuntimeException("UserId is required for payment processing");
        }

        log.info("Processing payment for orderId: {}, userId: {}", request.getOrderId(), request.getUserId());

        // ===== BƯỚC 1: VALIDATE ORDER =====
        OrderDetailResponse orderDetail = validateOrder(request);

        // ===== BƯỚC 2: VALIDATE USER =====
        validateUser(request.getUserId());

        // ===== BƯỚC 3: VALIDATE PAYMENT AMOUNT =====
        validatePaymentAmount(request, orderDetail);

        // ===== BƯỚC 4: CHECK IF PAYMENT ALREADY EXISTS =====
        Payment existingPayment = paymentRepository.findByOrderId(request.getOrderId())
                .orElse(null);

        Payment payment;
        if (existingPayment != null) {
            // Payment already exists - check if it's already processed
            if (existingPayment.getStatus() == Payment.Status.SUCCESS) {
                log.warn("Payment for orderId: {} already processed successfully. Checking if event was sent...", request.getOrderId());
                
                // Check if outbox event already exists for this payment (check all statuses)
                boolean eventExists = outboxEventRepository.findAll().stream()
                        .anyMatch(e -> "PAYMENT_SUCCESS".equals(e.getType()) 
                                && existingPayment.getId().toString().equals(e.getAggregateId()));
                
                if (!eventExists) {
                    // Event doesn't exist, create it now (idempotent - safe to recreate)
                    log.info("Payment SUCCESS but no event found. Creating PAYMENT_SUCCESS event for orderId: {}", request.getOrderId());
                    PaymentSuccessEventPayload payload = PaymentSuccessEventPayload.builder()
                            .paymentId(existingPayment.getId())
                            .orderId(existingPayment.getOrderId())
                            .build();
                    createOutboxEvent(existingPayment, "PAYMENT_SUCCESS", payload);
                } else {
                    log.info("Payment SUCCESS and event already exists (may be NEW or PROCESSED). Event will be published by OutboxEventRelay if status is NEW.");
                }
                
                return true;
            }
            if (existingPayment.getStatus() == Payment.Status.FAILED) {
                log.info("Payment for orderId: {} previously failed. Retrying payment.", request.getOrderId());
                payment = existingPayment;
                // Reset status to PENDING for retry
                payment.setStatus(Payment.Status.PENDING);
            } else {
                // Payment is still PENDING, update it
                payment = existingPayment;
            }
        } else {
            // Create new payment
            payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(request.getGrandTotal())
                    .currency(request.getCurrency())
                    .status(Payment.Status.PENDING)
                    .build();
        }

        payment = paymentRepository.save(payment);

        try {
            // TODO: Giả lập gọi cổng thanh toán
            log.info("Payment gateway processing simulation for orderId: {}", request.getOrderId());
            //Thread.sleep(1000); // Giả lập độ trễ

            // 5. Xử lý thành công
            payment.setStatus(Payment.Status.SUCCESS);
            payment.setTransactionNo("DUMMY_TXN_" + System.currentTimeMillis());
            paymentRepository.save(payment);

            log.info("Payment SUCCESS for orderId: {}", request.getOrderId());

            // 6. TẠO OUTBOX EVENT (để báo lại cho order-service)
            PaymentSuccessEventPayload payload = PaymentSuccessEventPayload.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .build();
            createOutboxEvent(payment, "PAYMENT_SUCCESS", payload);
            return true;

        } catch (Exception e) {
            log.error("Payment FAILED for orderId: {}", request.getOrderId(), e);

            // 7. Xử lý thất bại
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailReason(e.getMessage());
            paymentRepository.save(payment);

            // 8. TẠO OUTBOX EVENT (để báo lại cho order-service)
            PaymentFailedEventPayload payload = PaymentFailedEventPayload.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .reason(e.getMessage())
                    .build();
            createOutboxEvent(payment, "PAYMENT_FAILED", payload);
            return false;
        }
    }

    /**
     * Validate order exists, belongs to user, and is in CONFIRMED status
     */
    private OrderDetailResponse validateOrder(PaymentRequest request) {
        log.info("Validating order: orderId={}, userId={}", request.getOrderId(), request.getUserId());

        // Call Order Service to get order details
        OrderDetailResponse orderDetail = orderServicePort.getOrderDetail(request.getOrderId());

        // Validate order belongs to user
        if (!orderDetail.getUserId().equals(request.getUserId())) {
            log.error("Order {} does not belong to user {}. Order belongs to user {}", 
                    request.getOrderId(), request.getUserId(), orderDetail.getUserId());
            throw new RuntimeException("Order không thuộc về user này");
        }

        // Validate order status is CONFIRMED (user can only pay when order is confirmed)
        if (!"CONFIRMED".equalsIgnoreCase(orderDetail.getStatus())) {
            log.error("Order {} is in status {}, but payment can only be processed when order is CONFIRMED", 
                    request.getOrderId(), orderDetail.getStatus());
            throw new RuntimeException("Chỉ có thể thanh toán khi đơn hàng ở trạng thái CONFIRMED. Trạng thái hiện tại: " + orderDetail.getStatus());
        }

        log.info("Order validation passed: orderId={}, userId={}, status={}", 
                orderDetail.getId(), orderDetail.getUserId(), orderDetail.getStatus());
        return orderDetail;
    }

    /**
     * Validate user exists
     */
    private void validateUser(Long userId) {
        log.info("Validating user: userId={}", userId);

        UserValidationResponse userValidation = userServicePort.validateUser(userId);

        if (!userValidation.isExists()) {
            log.error("User {} does not exist", userId);
            throw new RuntimeException("User không tồn tại");
        }

        if (!userValidation.isActive()) {
            log.error("User {} is not active", userId);
            throw new RuntimeException("User không hoạt động");
        }

        log.info("User validation passed: userId={}, username={}", 
                userValidation.getUserId(), userValidation.getUsername());
    }

    /**
     * Validate payment amount matches order grand total
     */
    private void validatePaymentAmount(PaymentRequest request, OrderDetailResponse orderDetail) {
        log.info("Validating payment amount: requestAmount={}, orderGrandTotal={}", 
                request.getGrandTotal(), orderDetail.getGrandTotal());

        // Compare amounts (with tolerance for decimal precision)
        BigDecimal requestAmount = request.getGrandTotal();
        BigDecimal orderAmount = orderDetail.getGrandTotal();

        if (requestAmount.compareTo(orderAmount) != 0) {
            log.error("Payment amount {} does not match order grand total {}", 
                    requestAmount, orderAmount);
            throw new RuntimeException(
                    String.format("Số tiền thanh toán (%s) không khớp với tổng tiền đơn hàng (%s)", 
                            requestAmount, orderAmount)
            );
        }

        log.info("Payment amount validation passed: amount={}", requestAmount);
    }

    // Hàm tạo Outbox Event (y hệt bên order-service)
    private void createOutboxEvent(Payment payment, String eventType, Object payloadObject) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payloadObject);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .type(eventType)
                    .payload(payloadJson)
                    .status(EventStatus.NEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(event);

        } catch (JsonProcessingException e) {
            log.error("CRITICAL: Failed to serialize event payload for paymentId: {}. Transaction will be rolled back.", payment.getId(), e);
            throw new RuntimeException("Failed to create outbox event payload", e);
        }
    }

}

