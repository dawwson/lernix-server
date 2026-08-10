package com.lxp.aplus.payment.application.port.in.model.result;

import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentPrepareResult(
        String paymentId,
        String paymentAttemptId,
        String orderId,
        BigDecimal amount
) {
    public static PaymentPrepareResult from(Payment payment, PaymentAttempt attempt) {
        return PaymentPrepareResult.builder()
                .paymentId(payment.getId())
                .paymentAttemptId(attempt.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .build();
    }
}
