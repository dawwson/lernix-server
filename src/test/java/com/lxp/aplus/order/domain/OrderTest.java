package com.lxp.aplus.order.domain;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.ErrorCode;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order 도메인 테스트")
class OrderTest {

    @Test
    @DisplayName("주문을 생성하면 항목 가격 합계와 초기 상태가 설정된다")
    void create_courseItems_calculatesAmountAndInitializesOrder() {
        OrderItem firstItem = courseItem(10L, 10_000);
        OrderItem secondItem = courseItem(20L, 30_000);

        Order order = Order.create(1L, List.of(firstItem, secondItem));

        assertThat(order.getOrderId()).isNotBlank();
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getCurrency()).isEqualTo("KRW");
        assertThat(order.getAmount()).isEqualByComparingTo("40000");
        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(order.getOrderItems()).containsExactly(firstItem, secondItem);
        assertThat(order.getOrderItems()).allSatisfy(
                orderItem -> assertThat(orderItem.getOrder()).isSameAs(order)
        );
    }

    @Test
    @DisplayName("사용자나 주문 항목이 없으면 주문을 생성할 수 없다")
    void create_missingRequiredValue_throwsInvalidArgument() {
        assertOrderError(
                () -> Order.create(null, List.of(courseItem(10L, 10_000))),
                OrderErrorCode.ORDER_INVALID_USER
        );
        assertOrderError(
                () -> Order.create(1L, List.of()),
                OrderErrorCode.ORDER_INVALID_ITEM
        );
    }

    @Test
    @DisplayName("식별자나 가격이 유효하지 않으면 주문 항목을 생성할 수 없다")
    void createCourseItem_invalidValue_throwsInvalidItem() {
        assertOrderError(
                () -> OrderItem.createCourseItem(null, BigDecimal.valueOf(10_000)),
                OrderErrorCode.ORDER_INVALID_ITEM
        );
        assertOrderError(
                () -> OrderItem.createCourseItem(10L, BigDecimal.valueOf(-1)),
                OrderErrorCode.ORDER_INVALID_ITEM
        );
    }

    @Test
    @DisplayName("같은 주문 항목을 중복하거나 다른 주문에 재사용할 수 없다")
    void create_duplicatedOrAssignedItem_throwsInvalidArgument() {
        OrderItem item = courseItem(10L, 10_000);

        assertOrderError(
                () -> Order.create(1L, List.of(item, item)),
                OrderErrorCode.ORDER_INVALID_ITEM
        );

        Order.create(1L, List.of(item));

        assertOrderError(
                () -> Order.create(2L, List.of(item)),
                OrderErrorCode.ORDER_ITEM_ALREADY_ASSIGNED
        );
    }

    @Test
    @DisplayName("외부에서 주문 항목 컬렉션을 변경할 수 없다")
    void getOrderItems_cannotModifyItems() {
        Order order = order(40_000);

        assertThatThrownBy(() -> order.getOrderItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("대기 중인 주문의 결제를 승인하면 주문이 완료된다")
    void completeWithApprovedPayment_pendingOrder_completesOrder() {
        Order order = order(40_000);

        Order.CompletionResult result =
                order.completeWithApprovedPayment("payment-1", BigDecimal.valueOf(40_000));

        assertThat(result).isEqualTo(Order.CompletionResult.FIRST_COMPLETION);
        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.COMPLETED);
        assertThat(order.getApprovedPaymentId()).isEqualTo("payment-1");
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("승인 금액이 주문 총액과 다르면 주문 상태를 변경하지 않는다")
    void completeWithApprovedPayment_mismatchedAmount_keepsOrderPending() {
        Order order = order(40_000);

        assertOrderError(
                () -> order.completeWithApprovedPayment("payment-1", BigDecimal.valueOf(39_000)),
                OrderErrorCode.ORDER_AMOUNT_MISMATCH
        );
        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(order.getApprovedPaymentId()).isNull();
        assertThat(order.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("승인 결제 식별자나 금액이 없으면 주문을 완료할 수 없다")
    void completeWithApprovedPayment_missingRequiredValue_throwsInvalidArgument() {
        Order order = order(40_000);

        assertOrderError(
                () -> order.completeWithApprovedPayment(" ", BigDecimal.valueOf(40_000)),
                OrderErrorCode.ORDER_INVALID_PAYMENT_INFO
        );
        assertOrderError(
                () -> order.completeWithApprovedPayment("payment-1", null),
                OrderErrorCode.ORDER_INVALID_PAYMENT_INFO
        );
        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.PENDING);
    }

    @Test
    @DisplayName("동일한 결제로 완료된 주문을 다시 완료하면 기존 완료 결과를 반환한다")
    void completeWithApprovedPayment_samePayment_returnsAlreadyCompleted() {
        Order order = order(40_000);
        order.completeWithApprovedPayment("payment-1", BigDecimal.valueOf(40_000));

        Order.CompletionResult result =
                order.completeWithApprovedPayment("payment-1", BigDecimal.valueOf(40_000));

        assertThat(result).isEqualTo(Order.CompletionResult.ALREADY_COMPLETED);
        assertThat(order.getApprovedPaymentId()).isEqualTo("payment-1");
    }

    @Test
    @DisplayName("다른 결제로 완료된 주문을 다시 완료하면 결제 충돌 예외가 발생한다")
    void completeWithApprovedPayment_differentPayment_throwsPaymentConflict() {
        Order order = order(40_000);
        order.completeWithApprovedPayment("payment-1", BigDecimal.valueOf(40_000));

        assertOrderError(
                () -> order.completeWithApprovedPayment("payment-2", BigDecimal.valueOf(40_000)),
                OrderErrorCode.ORDER_PAYMENT_CONFLICT
        );
        assertThat(order.getApprovedPaymentId()).isEqualTo("payment-1");
    }

    @Test
    @DisplayName("대기 중인 주문을 취소하면 사유와 취소 상태가 저장된다")
    void cancel_pendingOrder_cancelsOrder() {
        Order order = order(40_000);

        order.cancel("사용자 요청");

        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo("사용자 요청");
    }

    @Test
    @DisplayName("취소 사유가 없으면 주문을 취소할 수 없다")
    void cancel_blankReason_throwsInvalidArgument() {
        Order order = order(40_000);

        assertOrderError(() -> order.cancel(" "), OrderErrorCode.ORDER_CANCEL_REASON_REQUIRED);

        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.PENDING);
    }

    @Test
    @DisplayName("완료된 주문을 취소하면 잘못된 상태 예외가 발생한다")
    void cancel_completedOrder_throwsInvalidStatus() {
        Order order = order(40_000);
        order.completeWithApprovedPayment("payment-1", BigDecimal.valueOf(40_000));

        assertOrderError(
                () -> order.cancel("사용자 요청"),
                OrderErrorCode.ORDER_INVALID_STATUS
        );
        assertThat(order.getOrderStatus()).isEqualTo(Order.Status.COMPLETED);
        assertThat(order.getCancelReason()).isNull();
    }

    private Order order(int price) {
        return Order.create(1L, List.of(courseItem(10L, price)));
    }

    private OrderItem courseItem(Long courseId, int price) {
        return OrderItem.createCourseItem(courseId, BigDecimal.valueOf(price));
    }

    private void assertOrderError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
