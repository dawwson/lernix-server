package com.lxp.aplus.order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO: OrderItem 내부로 이동
@Getter
@RequiredArgsConstructor
public enum ItemType {
    COURSE("강좌");

    private final String description;
}
