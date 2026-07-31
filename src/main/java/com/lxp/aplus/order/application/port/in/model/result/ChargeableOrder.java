package com.lxp.aplus.order.application.port.in.model.result;

import com.lxp.aplus.order.domain.Order;

import java.math.BigDecimal;

public record ChargeableOrder(
        String orderId,
        Long userId,
        BigDecimal amount
) {
    public static ChargeableOrder from(Order order) {
        return new ChargeableOrder(
                order.getOrderId(),
                order.getUserId(),
                order.getAmount()
        );
    }
}
