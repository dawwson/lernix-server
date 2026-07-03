package com.lxp.aplus.common.event;

import java.util.List;

public record OrderCompletedEvent(
        String orderId,
        Long userId,
        List<Item> items
) {
    public record Item(
            Long courseId,
            Long orderItemId
    ) {
    }
}
