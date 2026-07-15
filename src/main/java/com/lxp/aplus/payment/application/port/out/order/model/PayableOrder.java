package com.lxp.aplus.payment.application.port.out.order.model;

import java.math.BigDecimal;

public record PayableOrder(
        String orderId,
        Long userId,
        BigDecimal amount
) {
    public static PayableOrder of(String orderId, Long userId, BigDecimal amount) {
        return new PayableOrder(orderId, userId, amount);
    }
}
