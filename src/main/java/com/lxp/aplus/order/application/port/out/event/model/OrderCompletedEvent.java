package com.lxp.aplus.order.application.port.out.event.model;

import java.util.List;

/**
 * Order BC가 발행하는 주문 완료 이벤트입니다.
 */
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
