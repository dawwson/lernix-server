package com.lxp.aplus.payment.presentation.response;

import com.lxp.aplus.payment.application.result.PaymentPrepareResult;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentPrepareResponse(
        String orderId,
        BigDecimal amount
) {
    public static PaymentPrepareResponse from(PaymentPrepareResult result) {
        return PaymentPrepareResponse.builder()
                .orderId(result.orderId())
                .amount(result.amount())
                .build();
    }
}
