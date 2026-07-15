package com.lxp.aplus.payment.application.port.in.model.result;

import com.lxp.aplus.payment.domain.Payment;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentPrepareResult(
        String orderId,
        BigDecimal amount
) {
    public static PaymentPrepareResult from(Payment payment) {
        return PaymentPrepareResult.builder()
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .build();
    }
}
