package com.lxp.aplus.cart.application.port.in.model.result;

import com.lxp.aplus.cart.domain.Cart;
import com.lxp.aplus.cart.domain.CartItem;
import lombok.Builder;

@Builder
public record CartAddItemResult(
        Long cartId,
        Long cartItemId,
        int amount
) {
    public static CartAddItemResult of(Cart cart, CartItem addedCartItem, int amount) {
        return CartAddItemResult.builder()
                .cartId(cart.getId())
                .cartItemId(addedCartItem.getId())
                .amount(amount)
                .build();
    }
}
