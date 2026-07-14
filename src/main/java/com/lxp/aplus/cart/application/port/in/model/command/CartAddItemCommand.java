package com.lxp.aplus.cart.application.port.in.model.command;

import lombok.Builder;

@Builder
public record CartAddItemCommand(
        Long userId,
        Long courseId
) {
}
