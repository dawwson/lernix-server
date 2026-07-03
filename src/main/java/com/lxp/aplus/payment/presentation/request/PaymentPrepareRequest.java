package com.lxp.aplus.payment.presentation.request;

import com.lxp.aplus.payment.application.command.PaymentPrepareCommand;
import jakarta.validation.constraints.NotBlank;

public record PaymentPrepareRequest(
        @NotBlank(message = "orderId는 필수입니다.")
        String orderId
) {
    public PaymentPrepareCommand toCommand(Long userId) {
        return PaymentPrepareCommand.builder()
                .userId(userId)
                .orderId(orderId)
                .build();
    }
}
