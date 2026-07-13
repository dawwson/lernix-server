package com.lxp.aplus.cart.adapter.in.web.request;

import com.lxp.aplus.cart.application.command.CartAddItemCommand;
import jakarta.validation.constraints.NotNull;

public record CartAddItemRequest(
        @NotNull(message = "강좌 ID는 필수입니다.")
        Long courseId
) {
    public CartAddItemCommand toCommand(Long userId) {
        return CartAddItemCommand.builder()
                .userId(userId)
                .courseId(this.courseId)
                .build();
    }
}
