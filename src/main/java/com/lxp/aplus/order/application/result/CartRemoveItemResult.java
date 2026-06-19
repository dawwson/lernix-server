package com.lxp.aplus.order.application.result;

import com.lxp.aplus.order.domain.Cart;
import lombok.Builder;

@Builder
public record CartRemoveItemResult(
        Long cartId,
        Long removedCartItemId,
        int amount
) {
    public static CartRemoveItemResult of(Cart cart, Long removedCartItemId, int amount) {
        return CartRemoveItemResult.builder()
                .cartId(cart.getId())
                .removedCartItemId(removedCartItemId)
                .amount(amount)
                .build();
    }
}
