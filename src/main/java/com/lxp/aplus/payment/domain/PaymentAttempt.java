package com.lxp.aplus.payment.domain;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts", uniqueConstraints =
        @UniqueConstraint(name = "uk_payment_attempts_payment_key", columnNames = "payment_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PgProvider pgProvider;

    private LocalDateTime approvedAt;
    private LocalDateTime failedAt;

    private PaymentAttempt(String id, Payment payment) {
        this.id = id;
        this.payment = payment;
        this.status = Status.PENDING;
        this.paymentMethod = PaymentMethod.CARD;
        this.pgProvider = PgProvider.TOSS;
    }

    static PaymentAttempt create(Payment payment) {
        return new PaymentAttempt(UUID.randomUUID().toString(), payment);
    }

    public String getPaymentId() {
        return payment.getId();
    }

    void approve(String paymentKey) {
        validatePending();
        this.status = Status.APPROVED;
        this.paymentKey = paymentKey;
        this.approvedAt = LocalDateTime.now();
    }

    void fail() {
        validatePending();
        this.status = Status.FAILED;
        this.failedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    boolean matchesApproval(String paymentKey) {
        return status == Status.APPROVED && Objects.equals(this.paymentKey, paymentKey);
    }

    private void validatePending() {
        if (!isPending()) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_PENDING);
        }
    }

    public enum Status { PENDING, APPROVED, FAILED }

    @Getter
    @RequiredArgsConstructor
    public enum PaymentMethod {
        CARD("카드");

        private final String description;
    }

    @Getter
    @RequiredArgsConstructor
    public enum PgProvider {
        TOSS("토스페이먼츠");

        private final String description;
    }
}
