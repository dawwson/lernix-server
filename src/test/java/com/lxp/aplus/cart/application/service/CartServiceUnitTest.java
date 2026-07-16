package com.lxp.aplus.cart.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CartErrorCode;
import com.lxp.aplus.cart.application.port.in.model.command.CartAddItemCommand;
import com.lxp.aplus.cart.application.port.in.model.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.port.out.course.CartCourseQueryPort;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSalesStatus;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSnapshot;
import com.lxp.aplus.cart.application.port.out.repository.CartRepositoryPort;
import com.lxp.aplus.cart.application.port.in.model.result.CartAddItemResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartGetItemsResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartRemoveItemResult;
import com.lxp.aplus.cart.domain.Cart;
import com.lxp.aplus.cart.domain.CartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 단위 테스트")
class CartServiceUnitTest {

    @Mock
    private CartRepositoryPort cartRepository;

    @Mock
    private CartCourseQueryPort courseQueryPort;

    @InjectMocks
    private CartService cartService;

    private final Long USER_ID = 1L;

    // -------------------------------------------------------------------------
    // 1. 정상 흐름 테스트 (Happy Path)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("강좌를 장바구니에 추가하면 실시간 가격으로 총액이 계산되어야 한다")
    void addCartItem_Success() {
        // given
        Cart cart = Cart.create(USER_ID);
        Long courseId = 100L;
        int price = 50000;

        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(cartRepository.save(any())).willAnswer(inv -> {
            Cart savedCart = inv.getArgument(0);
            ReflectionTestUtils.setField(savedCart, "id", 10L);
            ReflectionTestUtils.setField(savedCart.getCartItems().get(0), "id", 20L);
            return savedCart;
        });

        given(courseQueryPort.getCourseSalesStatusByIds(anyList()))
                .willReturn(Map.of(courseId, new CourseSalesStatus(price, true)));

        // when
        CartAddItemResult result = cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, courseId));

        // then: 추가된 courseId와 계산된 금액이 정확한지 확인
        assertThat(result.cartId()).isEqualTo(10L);
        assertThat(result.cartItemId()).isEqualTo(20L);
        assertThat(result.amount()).isEqualTo(price);
        then(courseQueryPort).should().getCourseSalesStatusByIds(List.of(courseId));
        then(cartRepository).should().save(cart);
    }

    @Test
    @DisplayName("강좌 추가 시 기존 강좌와 신규 강좌를 한 번에 조회해 총액을 계산한다")
    void addCartItem_LoadsExistingAndNewCoursesOnce() {
        Cart cart = Cart.create(USER_ID);
        CartItem existingItem = cart.addCartItem(1L);
        ReflectionTestUtils.setField(cart, "id", 10L);
        ReflectionTestUtils.setField(existingItem, "id", 100L);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(cartRepository.save(cart)).willAnswer(invocation -> {
            Cart savedCart = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedCart.getCartItems().get(1), "id", 200L);
            return savedCart;
        });
        given(courseQueryPort.getCourseSalesStatusByIds(List.of(1L, 2L)))
                .willReturn(Map.of(
                        1L, new CourseSalesStatus(10000, true),
                        2L, new CourseSalesStatus(30000, true)
                ));

        CartAddItemResult result =
                cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, 2L));

        assertThat(result.cartItemId()).isEqualTo(200L);
        assertThat(result.amount()).isEqualTo(40000);
        then(courseQueryPort).should().getCourseSalesStatusByIds(List.of(1L, 2L));
    }

    @Test
    @DisplayName("아이템을 삭제하면 남은 강좌들의 금액 합계가 응답되어야 한다")
    void removeCartItem_Success() {
        // 1. given: 장바구니 생성 및 아이템 추가
        Cart cart = Cart.create(USER_ID);
        cart.addCartItem(1L); // 삭제 대상이 될 아이템
        cart.addCartItem(2L); // 남겨둘 아이템

        // 2. ReflectionTestUtils로 가짜 ID(PK) 주입
        // DB가 없는 단위 테스트 환경에서 getId()가 값을 반환하게 만듭니다.
        CartItem item1 = cart.getCartItems().get(0);
        CartItem item2 = cart.getCartItems().get(1);

        ReflectionTestUtils.setField(item1, "id", 100L); // 삭제할 아이템의 ID를 100으로 설정
        ReflectionTestUtils.setField(item2, "id", 200L); // 남을 아이템의 ID를 200으로 설정

        Long targetItemId = 100L; // 삭제 요청할 ID

        // 3. Mock 환경 설정
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        // 가격표 셋업: 삭제 후 남은 2번 강의(2L)의 가격이 필요함
        given(courseQueryPort.getCourseSalesStatusByIds(List.of(2L)))
                .willReturn(Map.of(2L, new CourseSalesStatus(30000, true)));

        // 4. when: 삭제 실행
        CartRemoveItemResult result = cartService.removeCartItemFromCart(
                new CartRemoveItemCommand(USER_ID, targetItemId));

        // 5. then: 검증
        // - 1번 아이템이 삭제되고, 2번 아이템(2L 강의)만 남아서 금액이 30,000원이어야 함
        assertThat(result.amount()).isEqualTo(30000);
        assertThat(cart.getCartItems()).hasSize(1);
        assertThat(cart.getCourseIds()).containsExactly(2L); // 실제로 2번 강의만 남았는지 확인
        then(courseQueryPort).should().getCourseSalesStatusByIds(List.of(2L));
    }

    @Test
    @DisplayName("장바구니 조회 시 항목과 총액이 응답되어야 한다")
    void getCartItems_Success() {
        // given
        Cart cart = Cart.create(USER_ID);
        cart.addCartItem(1L);
        cart.addCartItem(2L);

        CartItem item1 = cart.getCartItems().get(0);
        CartItem item2 = cart.getCartItems().get(1);

        ReflectionTestUtils.setField(cart, "id", 10L);
        ReflectionTestUtils.setField(item1, "id", 100L);
        ReflectionTestUtils.setField(item2, "id", 200L);

        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(courseQueryPort.getCourseSnapshotByIds(anyList()))
                .willReturn(Map.of(
                        1L, new CourseSnapshot(1L, "Java Basic", "PUBLISHED", "강사1", "thumb1", 10000, true),
                        2L, new CourseSnapshot(2L, "Spring Basic", "DRAFT", "강사2", "thumb2", 30000, false)
                ));

        // when
        CartGetItemsResult result = cartService.getCartItems(USER_ID);

        // then
        assertThat(result.cartId()).isEqualTo(10L);
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalAmount()).isEqualTo(10000);
        assertThat(result.items().get(0).cartItemId()).isEqualTo(100L);
        assertThat(result.items().get(0).courseTitle()).isEqualTo("Java Basic");
        assertThat(result.items().get(0).instructorName()).isEqualTo("강사1");
        assertThat(result.items().get(0).price()).isEqualTo(10000);
        assertThat(result.items().get(1).courseStatus()).isEqualTo("DRAFT");
        assertThat(result.items().get(1).price()).isEqualTo(30000);
        then(courseQueryPort).should().getCourseSnapshotByIds(cart.getCourseIds());
        then(courseQueryPort).should(never()).getCourseSalesStatusByIds(anyList());
    }

    @Test
    @DisplayName("빈 장바구니를 조회하면 Course 조회 없이 빈 결과를 반환한다")
    void getCartItems_EmptyCart() {
        Cart cart = Cart.create(USER_ID);
        ReflectionTestUtils.setField(cart, "id", 10L);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        CartGetItemsResult result = cartService.getCartItems(USER_ID);

        assertThat(result.cartId()).isEqualTo(10L);
        assertThat(result.items()).isEmpty();
        assertThat(result.totalAmount()).isZero();
        then(courseQueryPort).should(never()).getCourseSnapshotByIds(anyList());
        then(courseQueryPort).should(never()).getCourseSalesStatusByIds(anyList());
    }
    // -------------------------------------------------------------------------
    // 2. 필수 예외 테스트 (Edge Case - 장애 방지용)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("이미 장바구니에 있는 강좌를 또 담으려고 하면 예외가 발생해야 한다")
    void addCartItem_Duplicate_Fail() {
        // given: 이미 100번 강좌가 담겨있음
        Cart cart = Cart.create(USER_ID);
        cart.addCartItem(100L);
        given(courseQueryPort.getCourseSalesStatusByIds(anyList()))
                .willReturn(Map.of(100L, new CourseSalesStatus(50000, true)));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        // when & then: 중복 추가 시 도메인 규칙 위반으로 예외 발생 확인
        assertCartError(
                () -> cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, 100L)),
                CartErrorCode.CART_DUPLICATED_CART_ITEM
        );
        then(cartRepository).should(never()).save(cart);
    }

    @Test
    @DisplayName("구매할 수 없는 강좌는 장바구니에 추가할 수 없다")
    void addCartItem_UnpurchasableCourse_Fail() {
        Cart cart = Cart.create(USER_ID);
        Long courseId = 100L;
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(courseQueryPort.getCourseSalesStatusByIds(anyList()))
                .willReturn(Map.of(courseId, new CourseSalesStatus(50000, false)));

        assertCartError(
                () -> cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, courseId)),
                CartErrorCode.CART_CANNOT_ADD_UNPUBLISHED_COURSE
        );

        assertThat(cart.getCartItems()).isEmpty();
        then(cartRepository).should(never()).save(cart);
    }

    @Test
    @DisplayName("Course 조회 결과에 추가할 강좌가 없으면 장바구니에 추가할 수 없다")
    void addCartItem_MissingCourse_Fail() {
        Cart cart = Cart.create(USER_ID);
        Long courseId = 100L;
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(courseQueryPort.getCourseSalesStatusByIds(List.of(courseId)))
                .willReturn(Map.of());

        assertCartError(
                () -> cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, courseId)),
                CartErrorCode.CART_CANNOT_ADD_UNPUBLISHED_COURSE
        );

        assertThat(cart.getCartItems()).isEmpty();
        then(cartRepository).should(never()).save(cart);
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 항목을 삭제하려 하면 예외가 발생해야 한다")
    void removeCartItem_NotFound_Fail() {
        // given: 빈 장바구니
        Cart cart = Cart.create(USER_ID);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        // when & then: 없는 ID(999L) 삭제 시도
        assertCartError(
                () -> cartService.removeCartItemFromCart(new CartRemoveItemCommand(USER_ID, 999L)),
                CartErrorCode.CART_ITEM_NOT_FOUND
        );
        then(courseQueryPort).should(never()).getCourseSalesStatusByIds(anyList());
    }

    @Test
    @DisplayName("마지막 아이템을 삭제하면 Course 조회 없이 합계 0을 반환한다")
    void removeLastCartItem_ReturnsZeroWithoutCourseQuery() {
        Cart cart = Cart.create(USER_ID);
        CartItem cartItem = cart.addCartItem(1L);
        ReflectionTestUtils.setField(cartItem, "id", 100L);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        CartRemoveItemResult result = cartService.removeCartItemFromCart(
                new CartRemoveItemCommand(USER_ID, 100L)
        );

        assertThat(result.amount()).isZero();
        assertThat(result.removedCartItemId()).isEqualTo(100L);
        assertThat(cart.getCartItems()).isEmpty();
        then(courseQueryPort).should(never()).getCourseSalesStatusByIds(anyList());
    }

    private void assertCartError(Runnable action, CartErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
