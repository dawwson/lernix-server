package com.lxp.aplus.payment.application.port.in.model.command;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentConfirmCommand(
        Long userId,
        String orderId,
        String paymentKey,
        BigDecimal amount
) {
}
