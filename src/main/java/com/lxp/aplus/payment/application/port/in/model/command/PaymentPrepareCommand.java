package com.lxp.aplus.payment.application.port.in.model.command;

import lombok.Builder;

@Builder
public record PaymentPrepareCommand(
        Long userId,
        String orderId
) {
}
