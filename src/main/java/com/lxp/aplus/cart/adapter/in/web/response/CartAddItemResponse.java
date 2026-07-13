package com.lxp.aplus.cart.adapter.in.web.response;

import com.lxp.aplus.cart.application.result.CartAddItemResult;
import lombok.Builder;

@Builder
public record CartAddItemResponse(
        Long cartId,
        Long cartItemId,
        int amount
) {
    public static CartAddItemResponse from(CartAddItemResult result) {
        return new CartAddItemResponse(
                result.cartId(),
                result.cartItemId(),
                result.amount()
        );
    }
}
