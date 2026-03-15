package com.example.notificationservice.interfaces.rest;

import com.example.notificationservice.application.dto.PaymentEventDto;
import com.example.notificationservice.application.dto.TestEmailRequest;
import com.example.notificationservice.domain.port.EmailSenderPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/test/email")
@RequiredArgsConstructor
@Profile({"dev", "test"})
public class TestEmailController {

    private final EmailSenderPort emailSenderPort;

    /**
     * Test endpoint để gửi email thanh toán thành công
     * POST /api/v1/test/email/success
     */
    @PostMapping("/success")
    public ResponseEntity<String> testPaymentSuccessEmail(@Valid @RequestBody TestEmailRequest request) {
        try {
            log.info("Test endpoint: Sending payment success email to: {}", request.getEmail());

            PaymentEventDto eventDto = PaymentEventDto.builder()
                    .paymentId(request.getPaymentId())
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .transactionId(request.getTransactionId() != null ? request.getTransactionId() : "TEST_TXN_" + System.currentTimeMillis())
                    .paymentTime(Instant.now())
                    .status("SUCCESS")
                    .build();

            emailSenderPort.sendPaymentSuccessEmail(eventDto, request.getEmail());

            return ResponseEntity.ok("Payment success email sent successfully to: " + request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send test payment success email", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send email");
        }
    }

    /**
     * Test endpoint để gửi email thanh toán thất bại
     * POST /api/v1/test/email/failed
     */
    @PostMapping("/failed")
    public ResponseEntity<String> testPaymentFailedEmail(@Valid @RequestBody TestEmailRequest request) {
        try {
            log.info("Test endpoint: Sending payment failed email to: {}", request.getEmail());

            PaymentEventDto eventDto = PaymentEventDto.builder()
                    .paymentId(request.getPaymentId())
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .transactionId(request.getTransactionId())
                    .paymentTime(Instant.now())
                    .status("FAILED")
                    .failureReason(request.getFailureReason() != null ? request.getFailureReason() : "Lỗi thanh toán không xác định")
                    .build();

            emailSenderPort.sendPaymentFailedEmail(eventDto, request.getEmail());

            return ResponseEntity.ok("Payment failed email sent successfully to: " + request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send test payment failed email", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send email");
        }
    }

    /**
     * Test endpoint để gửi email hoàn tiền
     * POST /api/v1/test/email/refunded
     */
    @PostMapping("/refunded")
    public ResponseEntity<String> testPaymentRefundedEmail(@Valid @RequestBody TestEmailRequest request) {
        try {
            log.info("Test endpoint: Sending payment refunded email to: {}", request.getEmail());

            PaymentEventDto eventDto = PaymentEventDto.builder()
                    .paymentId(request.getPaymentId())
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .paymentTime(Instant.now())
                    .status("REFUNDED")
                    .failureReason(request.getFailureReason() != null ? request.getFailureReason() : "Hoàn tiền theo yêu cầu")
                    .build();

            emailSenderPort.sendPaymentRefundedEmail(eventDto, request.getEmail());

            return ResponseEntity.ok("Payment refunded email sent successfully to: " + request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send test payment refunded email", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send email");
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/test/email/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service Test Email API is running");
    }
}
