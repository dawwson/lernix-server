package com.lxp.aplus.order.adapter.in.web.request;

import com.lxp.aplus.order.application.port.in.model.command.OrderCreateCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "courseIds는 필수입니다.")
        @Size(min = 1, message = "최소 1개 이상의 강좌가 필요합니다.")
        List<Long> courseIds
) {
    public OrderCreateCommand toCommand(Long userId) {
        return OrderCreateCommand.builder()
                .userId(userId)
                .courseIds(courseIds)
                .build();
    }
}
