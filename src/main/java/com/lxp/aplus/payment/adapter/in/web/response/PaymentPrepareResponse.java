package com.lxp.aplus.payment.adapter.in.web.response;

import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentPrepareResponse(
        String paymentId,
        String paymentAttemptId,
        String orderId,
        BigDecimal amount
) {
    public static PaymentPrepareResponse from(PaymentPrepareResult result) {
        return PaymentPrepareResponse.builder()
                .paymentId(result.paymentId())
                .paymentAttemptId(result.paymentAttemptId())
                .orderId(result.orderId())
                .amount(result.amount())
                .build();
    }
}
