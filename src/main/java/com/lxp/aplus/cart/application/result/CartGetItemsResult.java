package com.lxp.aplus.cart.application.result;

import com.lxp.aplus.cart.application.port.out.CourseSnapshot;
import com.lxp.aplus.cart.domain.Cart;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record CartGetItemsResult(
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
    ) {
    }

    public static CartGetItemsResult of(Cart cart, Map<Long, CourseSnapshot> snapshotMap, int totalAmount) {
        List<Item> items = cart.getCartItems().stream()
                .map(cartItem -> {
                    CourseSnapshot courseSnapshot = snapshotMap.get(cartItem.getCourseId());

                    return Item.builder()
                            .cartItemId(cartItem.getId())
                            .courseId(courseSnapshot.courseId())
                            .courseTitle(courseSnapshot.courseTitle())
                            .courseStatus(courseSnapshot.courseStatus())
                            .instructorName(courseSnapshot.instructorName())
                            .thumbnailUrl(courseSnapshot.thumbnailUrl())
                            .price(courseSnapshot.price())
                            .build();
                })
                .toList();

        return CartGetItemsResult.builder()
                .cartId(cart.getId())
                .items(items)
                .totalAmount(totalAmount)
                .build();
    }

    public static CartGetItemsResult empty(Long cartId) {
        return new CartGetItemsResult(cartId, List.of(), 0);
    }
}
