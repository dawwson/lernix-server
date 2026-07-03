package com.lxp.aplus.payment.application.command;

import lombok.Builder;

@Builder
public record PaymentPrepareCommand(
        Long userId,
        String orderId
) {
}
