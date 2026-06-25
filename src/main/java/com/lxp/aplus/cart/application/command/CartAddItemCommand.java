package com.lxp.aplus.cart.application.command;

import lombok.Builder;

@Builder
public record CartAddItemCommand(
        Long userId,
        Long courseId
) {
}
