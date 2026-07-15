package com.lxp.aplus.payment.adapter.in.web.request;

import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
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
