package com.lxp.aplus.order.application.service;

import com.lxp.aplus.order.application.port.in.model.command.OrderCreateCommand;
import com.lxp.aplus.order.application.port.in.model.result.OrderCreateResult;
import com.lxp.aplus.order.application.port.out.course.CourseQueryPort;
import com.lxp.aplus.order.application.port.out.course.model.CoursePrice;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceUnitTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private CourseQueryPort courseQueryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 시 강좌 가격을 조회하고 주문을 저장해야 한다")
    void createOrder_Success() {
        // given
        Long userId = 1L;
        List<Long> courseIds = List.of(10L, 20L);

        given(courseQueryPort.getCoursePriceByIds(courseIds))
                .willReturn(List.of(
                        new CoursePrice(10L, 10000),
                        new CoursePrice(20L, 30000)
                ));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderCreateResult result = orderService.createOrder(new OrderCreateCommand(userId, courseIds));

        // then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(courseQueryPort).getCoursePriceByIds(courseIds);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(savedOrder.getOrderItems()).hasSize(2);
        assertThat(result.orderId()).isEqualTo(savedOrder.getOrderId());
        assertThat(result.amount()).isEqualByComparingTo(savedOrder.getAmount());
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

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        orderService.completeOrder(orderId, paymentId, amount);

        // then
        ArgumentCaptor<OrderCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

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
}
