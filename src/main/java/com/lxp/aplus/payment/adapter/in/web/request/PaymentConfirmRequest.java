package com.lxp.aplus.payment.adapter.in.web.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentConfirmRequest(
        @NotBlank(message = "paymentId는 필수입니다.")
        String paymentId,

        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,

        @NotNull(message = "amount는 필수입니다.")
        BigDecimal amount,

        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey
) {
    public PaymentConfirmCommand toCommand(Long userId) {

        return PaymentConfirmCommand.builder()
                .userId(userId)
                .paymentId(paymentId)
                .orderId(orderId)
                .amount(amount)
                .paymentKey(paymentKey)
                .build();
    }
}
