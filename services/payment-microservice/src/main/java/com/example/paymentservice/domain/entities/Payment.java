package com.example.paymentservice.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain entity representing a Payment.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    private Long id;
    private Long orderId;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    
    @Builder.Default
    private String currency = "VND";
    
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String transactionNo;
    private String failReason;
    private BigDecimal refundAmount;

    /**
     * Business logic: Refund a payment
     * 
     * @param refundAmount The amount to refund
     * @throws IllegalStateException if payment status is not SUCCESS
     * @throws IllegalArgumentException if refund amount exceeds payment amount
     */
    public void refund(BigDecimal refundAmount) {
        // 1. Check trạng thái
        if (this.status != Status.SUCCESS) {
            throw new IllegalStateException(
                    "Cannot refund payment with status: " + this.status
            );
        }

        // 2. Validate số tiền
        if (refundAmount.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount cannot exceed payment amount"
            );
        }
        // 3. Lưu số tiền refund
        this.refundAmount = refundAmount;
        // 4. Cập nhật trạng thái
        this.status = Status.REFUNDED;
    }

    /**
     * Business logic: Retry a failed payment
     * 
     * @throws IllegalStateException if payment status is not FAILED
     */
    public void retry() {
        if (this.status != Status.FAILED) {
            throw new IllegalStateException(
                "Cannot retry payment with status: " + this.status + 
                ". Only FAILED payments can be retried."
            );
        }
        this.status = Status.PENDING;
        this.failReason = null; // Clear previous failure reason
    }

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED,
        REFUNDED
    }
}
