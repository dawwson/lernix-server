package com.lxp.aplus.order.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.CourseErrorCode;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.order.application.port.in.model.command.OrderCreateCommand;
import com.lxp.aplus.order.application.port.in.model.result.OrderCreateResult;
import com.lxp.aplus.order.application.port.out.course.OrderCourseQueryPort;
import com.lxp.aplus.order.application.port.out.course.model.PurchasableCourse;
import com.lxp.aplus.order.application.port.out.event.OrderEventPublisherPort;
import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;
import com.lxp.aplus.order.application.port.out.repository.OrderRepositoryPort;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import com.lxp.aplus.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceUnitTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderCourseQueryPort courseQueryPort;

    @Mock
    private OrderEventPublisherPort eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 시 강좌 가격을 조회하고 주문을 저장해야 한다")
    void createOrder_Success() {
        // given
        Long userId = 1L;
        List<Long> courseIds = List.of(10L, 20L);

        given(courseQueryPort.getPurchasableCourses(courseIds))
                .willReturn(List.of(
                        new PurchasableCourse(10L, 10000),
                        new PurchasableCourse(20L, 30000)
                ));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderCreateResult result = orderService.createOrder(new OrderCreateCommand(userId, courseIds));

        // then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(courseQueryPort).getPurchasableCourses(courseIds);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(savedOrder.getOrderItems()).hasSize(2);
        assertThat(result.orderId()).isEqualTo(savedOrder.getOrderId());
        assertThat(result.amount()).isEqualByComparingTo(savedOrder.getAmount());
    }

    @Test
    @DisplayName("판매할 수 없는 강좌가 포함되면 주문을 저장하지 않는다")
    void createOrder_unpurchasableCourse_doesNotSaveOrder() {
        Long userId = 1L;
        List<Long> courseIds = List.of(10L, 20L);
        BusinessException exception = new BusinessException(CourseErrorCode.COURSE_NOT_PURCHASABLE);
        given(courseQueryPort.getPurchasableCourses(courseIds)).willThrow(exception);

        assertThatThrownBy(() -> orderService.createOrder(new OrderCreateCommand(userId, courseIds)))
                .isSameAs(exception);

        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 완료 시 OrderCompletedEvent를 발행해야 한다")
    void completeOrder_PublishOrderCompletedEvent_Success() {
        // given
        String orderId = "order-1";
        String paymentId = "payment-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(40000);

        OrderItem item1 = OrderItem.createCourseItem(10L, BigDecimal.valueOf(10000));
        OrderItem item2 = OrderItem.createCourseItem(20L, BigDecimal.valueOf(30000));
        ReflectionTestUtils.setField(item1, "orderItemId", 100L);
        ReflectionTestUtils.setField(item2, "orderItemId", 200L);

        Order order = Order.create(userId, List.of(item1, item2));
        ReflectionTestUtils.setField(order, "orderId", orderId);

        given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));

        // when
        orderService.completeOrder(orderId, paymentId, amount);

        // then
        ArgumentCaptor<OrderCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        OrderCompletedEvent event = eventCaptor.getValue();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getApprovedPaymentId()).isEqualTo(paymentId);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.items())
                .extracting(OrderCompletedEvent.Item::courseId, OrderCompletedEvent.Item::orderItemId)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(10L, 100L),
                        org.assertj.core.groups.Tuple.tuple(20L, 200L)
                );
    }

    @Test
    @DisplayName("완료할 주문이 없으면 주문 없음 예외가 발생하고 이벤트를 발행하지 않는다")
    void completeOrder_missingOrder_throwsOrderNotFoundWithoutEvent() {
        String orderId = "missing-order";
        given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.empty());

        assertOrderError(
                () -> orderService.completeOrder(
                        orderId,
                        "payment-1",
                        BigDecimal.valueOf(40_000)
                ),
                OrderErrorCode.ORDER_NOT_FOUND
        );

        then(eventPublisher).should(never()).publish(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName("승인 금액이 주문 총액과 다르면 이벤트를 발행하지 않는다")
    void completeOrder_mismatchedAmount_doesNotPublishEvent() {
        String orderId = "order-1";
        Order order = Order.create(
                1L,
                List.of(OrderItem.createCourseItem(10L, BigDecimal.valueOf(40_000)))
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));

        assertOrderError(
                () -> orderService.completeOrder(
                        orderId,
                        "payment-1",
                        BigDecimal.valueOf(39_000)
                ),
                OrderErrorCode.ORDER_AMOUNT_MISMATCH
        );

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getApprovedPaymentId()).isNull();
        then(eventPublisher).should(never()).publish(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName("동일한 결제로 완료된 주문을 다시 완료하면 주문 완료 이벤트를 발행하지 않는다")
    void completeOrder_samePayment_doesNotPublishEventAgain() {
        String orderId = "order-1";
        BigDecimal amount = BigDecimal.valueOf(40_000);
        Order order = Order.create(
                1L,
                List.of(OrderItem.createCourseItem(10L, amount))
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        order.completeWithApprovedPayment("payment-1", amount);
        given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));

        orderService.completeOrder(orderId, "payment-1", amount);

        assertThat(order.getApprovedPaymentId()).isEqualTo("payment-1");
        then(eventPublisher).should(never()).publish(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName("다른 결제로 완료된 주문을 다시 완료하면 충돌하고 주문 완료 이벤트를 발행하지 않는다")
    void completeOrder_differentPayment_throwsPaymentConflictWithoutEvent() {
        String orderId = "order-1";
        BigDecimal amount = BigDecimal.valueOf(40_000);
        Order order = Order.create(
                1L,
                List.of(OrderItem.createCourseItem(10L, amount))
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        order.completeWithApprovedPayment("payment-1", amount);
        given(orderRepository.findByIdForUpdate(orderId)).willReturn(Optional.of(order));

        assertOrderError(
                () -> orderService.completeOrder(orderId, "payment-2", amount),
                OrderErrorCode.ORDER_PAYMENT_CONFLICT
        );

        then(eventPublisher).should(never()).publish(any(OrderCompletedEvent.class));
    }

    private void assertOrderError(Runnable action, OrderErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
