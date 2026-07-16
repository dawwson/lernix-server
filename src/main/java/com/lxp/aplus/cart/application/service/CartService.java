package com.lxp.aplus.cart.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CartErrorCode;
import com.lxp.aplus.cart.application.port.in.model.command.CartAddItemCommand;
import com.lxp.aplus.cart.application.port.in.model.command.CartRemoveItemCommand;
import com.lxp.aplus.cart.application.port.in.CartUseCase;
import com.lxp.aplus.cart.application.port.out.course.CourseQueryPort;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSalesStatus;
import com.lxp.aplus.cart.application.port.out.course.model.CourseSnapshot;
import com.lxp.aplus.cart.application.port.out.repository.CartRepositoryPort;
import com.lxp.aplus.cart.application.port.in.model.result.CartAddItemResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartGetItemsResult;
import com.lxp.aplus.cart.application.port.in.model.result.CartRemoveItemResult;
import com.lxp.aplus.cart.domain.Cart;
import com.lxp.aplus.cart.domain.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService implements CartUseCase {

    private final CartRepositoryPort cartRepository;
    private final CourseQueryPort courseQueryPort;

    /*
     * 장바구니에 강좌 항목을 추가한다.
     * - 추가 후 장바구니의 실시간 상태를 반영하기 위해 전체 금액을 재계산한다.
     * - Port를 통해 외부 도메인(Course)의 가격 정보를 간접적으로 참조한다.
     */
    @Override
    public CartAddItemResult addCartItemToCart(CartAddItemCommand command) {
        Cart cart = getOrCreateCart(command.userId());

        Map<Long, CourseSalesStatus> courseSalesStatusMap =
                getSalesStatusIncluding(cart, command.courseId());

        validatePurchasable(command.courseId(), courseSalesStatusMap);

        CartItem addedCartItem = cart.addCartItem(command.courseId());

        cartRepository.save(cart);

        int amount = calculateTotalFromSalesStatus(cart, courseSalesStatusMap);

        return CartAddItemResult.of(cart, addedCartItem, amount);
    }

    /*
     * 장바구니에서 특정 항목을 제거한다.
     */
    @Override
    public CartRemoveItemResult removeCartItemFromCart(CartRemoveItemCommand command) {
        // 1. cart 조회 (없으면 생성)
        Cart cart = getOrCreateCart(command.userId());

        // 2. cart에서 항목 제거 (dirty checking)
        cart.removeCartItem(command.cartItemId());

        int amount = 0;
        if (!cart.getCartItems().isEmpty()) {
            Map<Long, CourseSalesStatus> courseSalesStatusMap =
                    courseQueryPort.getCourseSalesStatusByIds(cart.getCourseIds());
            amount = calculateTotalFromSalesStatus(cart, courseSalesStatusMap);
        }

        return CartRemoveItemResult.of(cart, command.cartItemId(), amount);
    }

    /*
     * 사용자별 장바구니를 조회하거나, 없을 경우 새 장바구니를 생성하여 반환한다.
     *
     * FIXME: 회원가입 시 장바구니 미리 생성하도록 수정
     * 1. GET 요청에서 자원 생성 책임을 분리하고, 조회 시점에 데이터 존재를 보장하기 위함
     * 2. CQRS 패턴을 지키기 위함
     */
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.create(userId)));
    }

    private Map<Long, CourseSalesStatus> getSalesStatusIncluding(
            Cart cart,
            Long courseId
    ) {
        List<Long> courseIds = Stream.concat(
                        cart.getCourseIds().stream(),
                        Stream.of(courseId)
                )
                .distinct()
                .toList();

        return courseQueryPort.getCourseSalesStatusByIds(courseIds);
    }

    private void validatePurchasable(
            Long courseId,
            Map<Long, CourseSalesStatus> courseSalesStatusMap
    ) {
        CourseSalesStatus courseSalesStatus = courseSalesStatusMap.get(courseId);

        if (courseSalesStatus == null || !courseSalesStatus.purchasable()) {
            throw new BusinessException(CartErrorCode.CART_CANNOT_ADD_UNPUBLISHED_COURSE);
        }
    }

    private int calculateTotalFromSalesStatus(
            Cart cart,
            Map<Long, CourseSalesStatus> courseSalesStatusMap
    ) {
        return cart.getCartItems().stream()
                .mapToInt(cartItem -> {
                    CourseSalesStatus courseSalesStatus = courseSalesStatusMap.get(cartItem.getCourseId());

                    if (courseSalesStatus != null && courseSalesStatus.purchasable()) {
                        return courseSalesStatus.price();
                    }

                    return 0;
                })
                .sum();
    }

    private int calculateTotalFromSnapshots(
            Cart cart,
            Map<Long, CourseSnapshot> courseSnapshotMap
    ) {
        return cart.getCartItems().stream()
                .map(CartItem::getCourseId)
                .map(courseSnapshotMap::get)
                .filter(courseSnapshot -> courseSnapshot != null && courseSnapshot.purchasable())
                .mapToInt(CourseSnapshot::price)
                .sum();
    }

    @Override
    public CartGetItemsResult getCartItems(Long userId) {

        // 1. 장바구니 조회
        Cart cart = getOrCreateCart(userId);

        // 2. 장바구니 항목 조회
        List<CartItem> cartItems = cart.getCartItems();

        // 3. 응답 DTO 변환 및 반환
        if (cartItems.isEmpty()) {
            return CartGetItemsResult.empty(cart.getId());
        }

        Map<Long, CourseSnapshot> courseSnapshotMap =
                courseQueryPort.getCourseSnapshotByIds(cart.getCourseIds());

        int totalAmount = calculateTotalFromSnapshots(cart, courseSnapshotMap);

        return CartGetItemsResult.of(cart, courseSnapshotMap, totalAmount);
    }
}
