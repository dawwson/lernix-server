package com.lxp.aplus.order.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.order.application.port.in.model.result.ChargeableOrder;
import com.lxp.aplus.order.application.port.out.repository.OrderRepositoryPort;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderQueryToPaymentService 단위 테스트")
class OrderQueryToPaymentServiceUnitTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @InjectMocks
    private OrderQueryToPaymentService service;

    @Test
    @DisplayName("PENDING 상태의 사용자 주문이면 결제 가능한 주문 정보를 반환한다")
    void getChargeableOrder_Success() {
        Long userId = 1L;
        Order order = createOrder(userId);
        given(orderRepository.findById(order.getOrderId())).willReturn(Optional.of(order));

        ChargeableOrder result = service.getChargeableOrder(order.getOrderId(), userId);

        then(orderRepository).should().findById(order.getOrderId());
        assertThat(result.orderId()).isEqualTo(order.getOrderId());
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.amount()).isEqualByComparingTo(order.getAmount());
    }

    @Test
    @DisplayName("주문이 없으면 ORDER_NOT_FOUND 예외를 발생시킨다")
    void getChargeableOrder_OrderNotFound() {
        String orderId = "order-1";
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertOrderError(
                () -> service.getChargeableOrder(orderId, 1L),
                OrderErrorCode.ORDER_NOT_FOUND
        );
    }

    @Test
    @DisplayName("주문 사용자와 요청 사용자가 다르면 ORDER_NOT_FOUND 예외를 발생시킨다")
    void getChargeableOrder_UserMismatch() {
        Order order = createOrder(1L);
        given(orderRepository.findById(order.getOrderId())).willReturn(Optional.of(order));

        assertOrderError(
                () -> service.getChargeableOrder(order.getOrderId(), 2L),
                OrderErrorCode.ORDER_NOT_FOUND
        );
    }

    @Test
    @DisplayName("PENDING 상태가 아니면 ORDER_INVALID_STATUS 예외를 발생시킨다")
    void getChargeableOrder_InvalidStatus() {
        Long userId = 1L;
        Order order = createOrder(userId);
        order.cancel("사용자 요청");
        given(orderRepository.findById(order.getOrderId())).willReturn(Optional.of(order));

        assertOrderError(
                () -> service.getChargeableOrder(order.getOrderId(), userId),
                OrderErrorCode.ORDER_INVALID_STATUS
        );
    }

    private Order createOrder(Long userId) {
        return Order.create(
                userId,
                List.of(OrderItem.createCourseItem(10L, BigDecimal.valueOf(50000)))
        );
    }

    private void assertOrderError(Runnable action, OrderErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
