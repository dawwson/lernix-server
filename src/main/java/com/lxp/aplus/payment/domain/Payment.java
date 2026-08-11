package com.lxp.aplus.payment.domain;

import com.lxp.aplus.common.domain.BaseAggregateRoot;
import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints =
        @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseAggregateRoot {

    @Id
    private String id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private String orderId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PaymentAttempt> attempts = new ArrayList<>();

    @Version
    private Long version;

    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;

    private Payment(String id, String orderId, Long userId, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.currency = "KRW";
        this.status = Status.UNPAID;
    }

    public static Payment create(String orderId, Long userId, BigDecimal amount) {
        if (orderId == null) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_INVALID_ORDER_ID);
        }
        return new Payment(UUID.randomUUID().toString(), orderId, userId, amount);
    }

    public PaymentAttempt prepareAttempt() {
        validateUnpaid();
        validateNoPendingAttempt();

        PaymentAttempt attempt = PaymentAttempt.create(this);
        this.attempts.add(attempt);
        return attempt;
    }

    public Optional<PaymentAttempt> getPendingAttempt() {
        return attempts.stream()
                .filter(PaymentAttempt::isPending)
                .findFirst();
    }

    public void approve(String attemptId, String paymentKey, BigDecimal approvedAmount) {
        PaymentAttempt attempt = getAttempt(attemptId);
        validateUnpaid();
        validatePendingAttempt(attempt);
        validateAmount(approvedAmount);
        attempt.approve(paymentKey);
        this.status = Status.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void fail(PaymentAttempt attempt) {
        validateUnpaid();
        validatePendingAttempt(attempt);
        attempt.fail();
    }

    public void cancel() {
        validatePaid();
        this.status = Status.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void refund() {
        validatePaid();
        this.status = Status.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    public void validateOwner(Long userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ACCESS_DENIED);
        }
    }

    public void validateOrder(String orderId) {
        if (!Objects.equals(this.orderId, orderId)) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ORDER_MISMATCH);
        }
    }

    public void validateUnpaid() {
        if (status != Status.UNPAID) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);
        }
    }

    private void validatePaid() {
        if (status != Status.PAID) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_APPROVED);
        }
    }

    private void validateNoPendingAttempt() {
        if (getPendingAttempt().isPresent()) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);
        }
    }

    private void validatePendingAttempt(PaymentAttempt attempt) {
        if (attempt == null || !attempts.contains(attempt) || !attempt.isPending()
                || !Objects.equals(id, attempt.getPaymentId())) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_NOT_PENDING);
        }
    }

    private void validateAmount(BigDecimal approvedAmount) {
        if (amount.compareTo(approvedAmount) != 0) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private PaymentAttempt getAttempt(String attemptId) {
        return attempts.stream()
                .filter(attempt -> Objects.equals(attempt.getId(), attemptId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    public enum Status { UNPAID, PAID, CANCELED, REFUNDED }
}
