package com.lxp.aplus.cart.application.port.in.model.command;

import lombok.Builder;

@Builder
public record CartRemoveItemCommand(
        Long userId,
        Long cartItemId
) {
    public static CartRemoveItemCommand of(Long userId, Long cartItemId) {
        return new CartRemoveItemCommand(userId, cartItemId);
    }
}
