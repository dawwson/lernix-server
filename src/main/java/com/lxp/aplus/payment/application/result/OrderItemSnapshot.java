package com.lxp.aplus.payment.application.result;

public record OrderItemSnapshot(
        Long courseId,
        Long orderItemId
) {
    public static OrderItemSnapshot of(Long courseId, Long orderItemId) {
        return new OrderItemSnapshot(courseId, orderItemId);
    }
}
