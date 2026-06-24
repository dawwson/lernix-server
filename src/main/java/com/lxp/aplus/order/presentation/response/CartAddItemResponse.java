package com.lxp.aplus.order.presentation.response;

import com.lxp.aplus.order.application.result.CartAddItemResult;
import lombok.Builder;

@Builder
public record CartAddItemResponse(
        Long cartId,
        Long cartItemId,
        int amount
) {
    public static CartAddItemResponse from(CartAddItemResult result) {
        return CartAddItemResponse.builder()
                .cartId(result.cartId())
                .cartItemId(result.cartItemId())
                .amount(result.amount())
                .build();
    }
}
