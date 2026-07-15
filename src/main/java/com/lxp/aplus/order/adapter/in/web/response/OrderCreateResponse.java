package com.lxp.aplus.order.adapter.in.web.response;

import com.lxp.aplus.order.application.port.in.model.result.OrderCreateResult;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderCreateResponse(
        String orderId,
        BigDecimal amount
) {
    public static OrderCreateResponse from(OrderCreateResult result) {
        return OrderCreateResponse.builder()
                .orderId(result.orderId())
                .amount(result.amount())
                .build();
    }
}
