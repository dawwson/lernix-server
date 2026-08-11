package com.lxp.aplus.integration.purchase;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.enrollment.application.command.EnrollmentCommand;
import com.lxp.aplus.enrollment.application.port.in.EnrollmentCommandUseCase;
import com.lxp.aplus.order.adapter.out.persistence.OrderJpaRepository;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import com.lxp.aplus.payment.adapter.out.persistence.PaymentJpaRepository;
import com.lxp.aplus.payment.application.port.in.PaymentUseCase;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Payment와 Order 이벤트 통합 테스트")
class PurchaseCompletionEventIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(40_000);

    @Autowired
    private PaymentUseCase paymentUseCase;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @MockitoBean
    private EnrollmentCommandUseCase enrollmentCommandUseCase;

    private Order order;
    private Payment payment;
    private PaymentAttempt attempt;
    private Long orderItemId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();

        order = Order.create(
                USER_ID,
                List.of(OrderItem.createCourseItem(COURSE_ID, AMOUNT))
        );
        order = orderRepository.saveAndFlush(order);
        orderItemId = order.getOrderItems().get(0).getOrderItemId();

        payment = Payment.create(order.getOrderId(), USER_ID, AMOUNT);
        attempt = payment.prepareAttempt();
        payment = paymentRepository.saveAndFlush(payment);
    }

    @Test
    @DisplayName("결제 트랜잭션이 커밋되면 주문을 완료하고 수강 등록 정보를 전달한다")
    void confirmPayment_committedTransaction_completesOrderAndPublishesEnrollmentCommand() {
        paymentUseCase.confirm(new PaymentConfirmCommand(
                USER_ID,
                payment.getId(),
                attempt.getId(),
                order.getOrderId(),
                "payment-key",
                AMOUNT
        ));

        Payment approvedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        PaymentAttempt approvedAttempt = approvedPayment.getAttempts().stream()
                .filter(candidate -> candidate.getId().equals(attempt.getId()))
                .findFirst()
                .orElseThrow();
        Order completedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();

        assertThat(approvedPayment.getStatus()).isEqualTo(Payment.Status.PAID);
        assertThat(approvedAttempt.getPaymentKey()).isEqualTo("payment-key");
        assertThat(completedOrder.getOrderStatus()).isEqualTo(Order.Status.COMPLETED);
        assertThat(completedOrder.getApprovedPaymentId()).isEqualTo(payment.getId());
        verify(enrollmentCommandUseCase, timeout(3_000)).enroll(
                new EnrollmentCommand(USER_ID, COURSE_ID, orderItemId)
        );
    }

    @Test
    @DisplayName("결제 승인 금액이 다르면 주문 상태를 변경하지 않는다")
    void confirmPayment_mismatchedAmount_keepsOrderPending() {
        assertThatThrownBy(() -> paymentUseCase.confirm(new PaymentConfirmCommand(
                USER_ID,
                payment.getId(),
                attempt.getId(),
                order.getOrderId(),
                "payment-key",
                BigDecimal.valueOf(39_000)
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);

        Payment pendingPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        PaymentAttempt pendingAttempt = pendingPayment.getAttempts().stream()
                .filter(candidate -> candidate.getId().equals(attempt.getId()))
                .findFirst()
                .orElseThrow();
        Order pendingOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(pendingPayment.getStatus()).isEqualTo(Payment.Status.UNPAID);
        assertThat(pendingAttempt.getPaymentKey()).isNull();
        assertThat(pendingOrder.getOrderStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(pendingOrder.getApprovedPaymentId()).isNull();
    }
}
