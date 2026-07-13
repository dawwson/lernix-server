package com.lxp.aplus.cart.adapter.in.web.response;

import com.lxp.aplus.cart.application.result.CartGetItemsResult;
import lombok.Builder;

import java.util.List;

@Builder
public record CartGetItemsResponse(
        Long cartId,
        List<Item> items,
        int totalAmount
) {
    @Builder
    public record Item(
            Long cartItemId,
            Long courseId,
            String courseTitle,
            String courseStatus,
            String instructorName,
            String thumbnailUrl,
            int price
    ) {}

    public static CartGetItemsResponse from(CartGetItemsResult result) {
        List<Item> items = result.items().stream()
                .map(item -> Item.builder()
                        .cartItemId(item.cartItemId())
                        .courseId(item.courseId())
                        .courseTitle(item.courseTitle())
                        .courseStatus(item.courseStatus())
                        .instructorName(item.instructorName())
                        .thumbnailUrl(item.thumbnailUrl())
                        .price(item.price())
                        .build())
                .toList();

        return CartGetItemsResponse.builder()
                .cartId(result.cartId())
                .items(items)
                .totalAmount(result.totalAmount())
                .build();
    }
}
