package com.lxp.aplus.cart.adapter.in.web.response;

import com.lxp.aplus.cart.application.port.in.model.result.CartRemoveItemResult;
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
