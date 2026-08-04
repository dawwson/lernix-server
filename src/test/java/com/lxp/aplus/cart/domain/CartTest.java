package com.lxp.aplus.cart.domain;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CartErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cart 도메인 테스트")
class CartTest {

    @Test
    @DisplayName("사용자 ID로 생성한 장바구니는 비어 있다")
    void create_userId_createsEmptyCart() {
        Cart cart = Cart.create(1L);

        assertThat(cart.getUserId()).isEqualTo(1L);
        assertThat(cart.getCartItems()).isEmpty();
        assertThat(cart.getCourseIds()).isEmpty();
    }

    @Test
    @DisplayName("강좌를 추가하면 장바구니 항목과 강좌 ID가 저장된다")
    void addCartItem_newCourse_addsCartItem() {
        Cart cart = Cart.create(1L);

        CartItem addedItem = cart.addCartItem(100L);

        assertThat(addedItem.getCart()).isSameAs(cart);
        assertThat(addedItem.getCourseId()).isEqualTo(100L);
        assertThat(cart.getCartItems()).containsExactly(addedItem);
        assertThat(cart.getCourseIds()).containsExactly(100L);
    }

    @Test
    @DisplayName("이미 담긴 강좌를 다시 추가하면 중복 항목 예외가 발생한다")
    void addCartItem_duplicateCourse_throwsDuplicatedCartItem() {
        Cart cart = Cart.create(1L);
        cart.addCartItem(100L);

        assertCartError(
                () -> cart.addCartItem(100L),
                CartErrorCode.CART_DUPLICATED_CART_ITEM
        );
        assertThat(cart.getCartItems()).hasSize(1);
    }

    @Test
    @DisplayName("장바구니 항목을 제거하면 해당 강좌도 목록에서 제외된다")
    void removeCartItem_existingItem_removesItem() {
        Cart cart = Cart.create(1L);
        CartItem item = cart.addCartItem(100L);
        ReflectionTestUtils.setField(item, "id", 10L);

        cart.removeCartItem(10L);

        assertThat(cart.getCartItems()).isEmpty();
        assertThat(cart.getCourseIds()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 항목을 제거하면 예외가 발생한다")
    void removeCartItem_missingItem_throwsCartItemNotFound() {
        Cart cart = Cart.create(1L);
        CartItem item = cart.addCartItem(100L);
        ReflectionTestUtils.setField(item, "id", 10L);

        assertCartError(
                () -> cart.removeCartItem(999L),
                CartErrorCode.CART_ITEM_NOT_FOUND
        );
        assertThat(cart.getCartItems()).containsExactly(item);
    }

    @Test
    @DisplayName("외부에서는 장바구니 항목 목록을 변경할 수 없다")
    void getCartItems_externalMutation_throwsUnsupportedOperationException() {
        Cart cart = Cart.create(1L);
        cart.addCartItem(100L);

        assertThatThrownBy(() -> cart.getCartItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(cart.getCartItems()).hasSize(1);
    }

    private void assertCartError(Runnable action, CartErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
