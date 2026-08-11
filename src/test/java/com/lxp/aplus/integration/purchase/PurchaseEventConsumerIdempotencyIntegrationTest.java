package com.lxp.aplus.integration.purchase;

import com.lxp.aplus.enrollment.application.command.EnrollmentCommand;
import com.lxp.aplus.enrollment.application.listener.OrderCompletedEventListener;
import com.lxp.aplus.enrollment.application.port.in.EnrollmentCommandUseCase;
import com.lxp.aplus.enrollment.application.port.out.CourseQueryPort;
import com.lxp.aplus.enrollment.application.port.out.CourseSummary;
import com.lxp.aplus.enrollment.infrastructure.persistence.EnrollmentJpaRepository;
import com.lxp.aplus.order.adapter.in.event.PaymentCompletedEventListener;
import com.lxp.aplus.order.adapter.out.persistence.OrderJpaRepository;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import com.lxp.aplus.order.application.port.out.event.OrderEventPublisherPort;
import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("구매 이벤트 소비 멱등성 통합 테스트")
class PurchaseEventConsumerIdempotencyIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long FIRST_COURSE_ID = 10L;
    private static final Long SECOND_COURSE_ID = 20L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(40_000);

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private EnrollmentCommandUseCase enrollmentCommandUseCase;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private EnrollmentJpaRepository enrollmentRepository;

    @MockitoBean
    private OrderEventPublisherPort orderEventPublisher;

    @MockitoBean
    private CourseQueryPort courseQueryPort;

    private PaymentCompletedEventListener paymentCompletedEventListener;
    private OrderCompletedEventListener orderCompletedEventListener;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        orderRepository.deleteAll();
        paymentCompletedEventListener = new PaymentCompletedEventListener(orderUseCase);
        orderCompletedEventListener = new OrderCompletedEventListener(enrollmentCommandUseCase);
    }

    @Test
    @DisplayName("동일 결제 완료 이벤트를 반복 처리해도 주문 완료 이벤트는 한 번만 발행한다")
    void duplicatePaymentCompletedEvent_publishesOrderCompletedEventOnce() {
        Order order = orderRepository.saveAndFlush(Order.create(
                USER_ID,
                List.of(OrderItem.createCourseItem(FIRST_COURSE_ID, AMOUNT))
        ));
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "payment-1",
                order.getOrderId(),
                USER_ID,
                AMOUNT
        );

        paymentCompletedEventListener.handlePaymentCompletedEvent(event);
        paymentCompletedEventListener.handlePaymentCompletedEvent(event);

        Order completedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(completedOrder.getOrderStatus()).isEqualTo(Order.Status.COMPLETED);
        assertThat(completedOrder.getApprovedPaymentId()).isEqualTo("payment-1");
        verify(orderEventPublisher, times(1)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("동일 주문 완료 이벤트를 반복 처리해도 수강권은 하나만 존재한다")
    void duplicateOrderCompletedEvent_createsEnrollmentOnce() {
        given(courseQueryPort.findCourseById(FIRST_COURSE_ID))
                .willReturn(Optional.of(new CourseSummary(FIRST_COURSE_ID, "첫 번째 강좌")));
        OrderCompletedEvent event = new OrderCompletedEvent(
                "order-1",
                USER_ID,
                List.of(new OrderCompletedEvent.Item(FIRST_COURSE_ID, 100L))
        );

        orderCompletedEventListener.handleOrderCompletedEvent(event);
        orderCompletedEventListener.handleOrderCompletedEvent(event);

        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(enrollmentRepository.findByOrderItemId(100L)).isPresent();
    }

    @Test
    @DisplayName("일부 수강권 생성 후 이벤트를 재처리하면 누락된 수강권만 생성한다")
    void partiallyProcessedOrderCompletedEvent_createsOnlyMissingEnrollment() {
        // given
        given(courseQueryPort.findCourseById(FIRST_COURSE_ID))
                .willReturn(Optional.of(new CourseSummary(FIRST_COURSE_ID, "첫 번째 강좌")));
        given(courseQueryPort.findCourseById(SECOND_COURSE_ID))
                .willReturn(Optional.of(new CourseSummary(SECOND_COURSE_ID, "두 번째 강좌")));
        enrollmentCommandUseCase.enroll(new EnrollmentCommand(USER_ID, FIRST_COURSE_ID, 100L));

        // when
        OrderCompletedEvent event = new OrderCompletedEvent(
                "order-1",
                USER_ID,
                List.of(
                        new OrderCompletedEvent.Item(FIRST_COURSE_ID, 100L),
                        new OrderCompletedEvent.Item(SECOND_COURSE_ID, 200L)
                )
        );

        orderCompletedEventListener.handleOrderCompletedEvent(event);

        // then
        assertThat(enrollmentRepository.count()).isEqualTo(2);
        assertThat(enrollmentRepository.findByOrderItemId(100L)).isPresent();
        assertThat(enrollmentRepository.findByOrderItemId(200L)).isPresent();
    }
}
