package com.lxp.aplus.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO: Order 내부로 이동
@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("주문 대기"),
    COMPLETED("주문 완료"),
    CANCELED("주문 취소");

    private final String description;
}
