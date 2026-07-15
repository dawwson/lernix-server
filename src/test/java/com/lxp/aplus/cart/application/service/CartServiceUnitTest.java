package com.lxp.aplus.cart.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.cart.application.port.in.model.command.CartAddItemCommand;
import com.lxp.aplus.cart.application.port.in.model.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.port.out.course.CourseQueryPort;
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

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 단위 테스트")
class CartServiceUnitTest {

    @Mock
    private CartRepositoryPort cartRepository;

    @Mock
    private CourseQueryPort courseQueryPort;

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

        given(courseQueryPort.isCoursePublished(courseId)).willReturn(true);
        given(courseQueryPort.getCourseSalesStatusByIds(anyList()))
                .willReturn(Map.of(courseId, new CourseSalesStatus(price, true)));

        // when
        CartAddItemResult result = cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, courseId));

        // then: 추가된 courseId와 계산된 금액이 정확한지 확인
        assertThat(result.cartId()).isEqualTo(10L);
        assertThat(result.cartItemId()).isEqualTo(20L);
        assertThat(result.amount()).isEqualTo(price);
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
        given(courseQueryPort.getCourseSalesStatusByIds(anyList()))
                .willReturn(Map.of(
                        1L, new CourseSalesStatus(10000, true),
                        2L, new CourseSalesStatus(30000, true)
                ));

        // 4. when: 삭제 실행
        CartRemoveItemResult result = cartService.removeCartItemFromCart(
                new CartRemoveItemCommand(USER_ID, targetItemId));

        // 5. then: 검증
        // - 1번 아이템이 삭제되고, 2번 아이템(2L 강의)만 남아서 금액이 30,000원이어야 함
        assertThat(result.amount()).isEqualTo(30000);
        assertThat(cart.getCartItems()).hasSize(1);
        assertThat(cart.getCourseIds()).containsExactly(2L); // 실제로 2번 강의만 남았는지 확인
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
        given(courseQueryPort.getCourseSnapshot(anyList()))
                .willReturn(Map.of(
                        1L, new CourseSnapshot(1L, "Java Basic", "PUBLISHED", "강사1", "thumb1", 10000),
                        2L, new CourseSnapshot(2L, "Spring Basic", "PUBLISHED", "강사2", "thumb2", 30000)
                ));
        given(courseQueryPort.getCourseSalesStatusByIds(anyList()))
                .willReturn(Map.of(
                        1L, new CourseSalesStatus(10000, true),
                        2L, new CourseSalesStatus(30000, true)
                ));

        // when
        CartGetItemsResult result = cartService.getCartItems(USER_ID);

        // then
        assertThat(result.cartId()).isEqualTo(10L);
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalAmount()).isEqualTo(40000);
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
        given(courseQueryPort.isCoursePublished(100L)).willReturn(true);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        // when & then: 중복 추가 시 도메인 규칙 위반으로 예외 발생 확인
        assertThatThrownBy(() ->
                cartService.addCartItemToCart(new CartAddItemCommand(USER_ID, 100L))
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 장바구니 항목을 삭제하려 하면 예외가 발생해야 한다")
    void removeCartItem_NotFound_Fail() {
        // given: 빈 장바구니
        Cart cart = Cart.create(USER_ID);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        // when & then: 없는 ID(999L) 삭제 시도
        assertThatThrownBy(() ->
                cartService.removeCartItemFromCart(new CartRemoveItemCommand(USER_ID, 999L))
        ).isInstanceOf(BusinessException.class);
    }
}
