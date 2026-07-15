package com.lxp.aplus.order.application.port.in.model.command;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderCreateCommand(
        Long userId,
        List<Long> courseIds
) {
}
