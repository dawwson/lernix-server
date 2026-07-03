package com.lxp.aplus.order.application.command;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderCreateCommand(
        Long userId,
        List<Long> courseIds
) {
}
