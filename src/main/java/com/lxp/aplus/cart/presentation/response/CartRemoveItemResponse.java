package com.lxp.aplus.cart.presentation.response;

import com.lxp.aplus.cart.application.result.CartRemoveItemResult;
import lombok.Builder;

@Builder
public record CartRemoveItemResponse(
        Long cartId,
        Long removedCartItemId,
        int amount
) {
    public static CartRemoveItemResponse from(CartRemoveItemResult result) {
        return CartRemoveItemResponse.builder()
                .cartId(result.cartId())
                .removedCartItemId(result.removedCartItemId())
                .amount(result.amount())
                .build();
    }
}
