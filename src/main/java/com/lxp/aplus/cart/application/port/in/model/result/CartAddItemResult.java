package com.lxp.aplus.cart.application.port.in.model.result;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CartErrorCode;
import com.lxp.aplus.cart.domain.Cart;
import com.lxp.aplus.cart.domain.CartItem;
import lombok.Builder;

@Builder
public record CartAddItemResult(
        Long cartId,
        Long cartItemId,
        int amount
) {
    // FIXME: Result 생성을 위해 Aggregate를 재탐색하고 있음. addItem() 반환값(CartItem)을 활용하도록 리팩토링 필요.
    public static CartAddItemResult of(Cart cart, Long addedCourseId, int amount) {
        CartItem addedCartItem = cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getCourseId().equals(addedCourseId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CartErrorCode.CART_ITEM_NOT_FOUND));

        return CartAddItemResult.builder()
                .cartId(cart.getId())
                .cartItemId(addedCartItem.getId())
                .amount(amount)
                .build();
    }
}
