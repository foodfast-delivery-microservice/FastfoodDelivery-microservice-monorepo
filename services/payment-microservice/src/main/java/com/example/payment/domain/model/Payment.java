package com.example.payment.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "VND";


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "transaction_no", length = 100) // Mã giao dịch từ cổng thanh toán
    private String transactionNo;

    @Column(name = "fail_reason", length = 255) // Lý do thất bại
    private String failReason;

    @Column(name = "refund_amount", precision = 12, scale = 2) // Số tiền đã hoàn
    private BigDecimal refundAmount;
    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = Status.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
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



