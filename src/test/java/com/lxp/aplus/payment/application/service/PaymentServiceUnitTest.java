package com.lxp.aplus.payment.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.in.model.result.PaymentPrepareResult;
import com.lxp.aplus.payment.application.port.out.event.PaymentEventPublisherPort;
import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
import com.lxp.aplus.payment.application.port.out.repository.PaymentRepositoryPort;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceUnitTest {

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private PaymentOrderQueryPort orderQueryPort;

    @Mock
    private PaymentEventPublisherPort eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제를 준비하면 주문 금액으로 결제를 생성하고 저장한다")
    void prepare_payableOrder_savesPaymentAndReturnsResult() {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50_000);
        given(orderQueryPort.getPayableOrder(orderId, userId))
                .willReturn(new PayableOrder(orderId, userId, amount));
        given(paymentRepository.findAllByOrderId(orderId)).willReturn(List.of());

        PaymentPrepareResult result = paymentService.prepare(
                new PaymentPrepareCommand(userId, orderId)
        );

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        then(orderQueryPort).should().getPayableOrder(orderId, userId);
        then(paymentRepository).should().save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getUserId()).isEqualTo(userId);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(amount);
        assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result).isEqualTo(new PaymentPrepareResult(savedPayment.getPaymentId(), orderId, amount));
    }

    @Test
    @DisplayName("결제할 수 없는 주문이면 결제를 저장하지 않는다")
    void prepare_unpayableOrder_doesNotSavePayment() {
        String orderId = "order-1";
        Long userId = 1L;
        BusinessException exception = new BusinessException(OrderErrorCode.ORDER_INVALID_STATUS);
        given(orderQueryPort.getPayableOrder(orderId, userId)).willThrow(exception);

        assertThatThrownBy(() -> paymentService.prepare(
                new PaymentPrepareCommand(userId, orderId)
        )).isSameAs(exception);

        then(paymentRepository).should(never()).findAllByOrderId(any(String.class));
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("대기 중인 결제가 있으면 새로 저장하지 않고 기존 결제를 반환한다")
    void prepare_pendingPayment_returnsExistingPayment() {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment pendingPayment = Payment.create(orderId, userId, amount);
        given(orderQueryPort.getPayableOrder(orderId, userId))
                .willReturn(new PayableOrder(orderId, userId, amount));
        given(paymentRepository.findAllByOrderId(orderId)).willReturn(List.of(pendingPayment));

        PaymentPrepareResult result = paymentService.prepare(new PaymentPrepareCommand(userId, orderId));

        assertThat(result).isEqualTo(PaymentPrepareResult.from(pendingPayment));
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("실패 이력과 대기 중인 결제가 함께 있으면 대기 중인 결제를 반환한다")
    void prepare_failedAndPendingPayments_returnsPendingPayment() {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment failedPayment = Payment.create(orderId, userId, amount);
        failedPayment.fail();
        Payment pendingPayment = Payment.create(orderId, userId, amount);
        given(orderQueryPort.getPayableOrder(orderId, userId))
                .willReturn(new PayableOrder(orderId, userId, amount));
        given(paymentRepository.findAllByOrderId(orderId))
                .willReturn(List.of(failedPayment, pendingPayment));

        PaymentPrepareResult result = paymentService.prepare(
                new PaymentPrepareCommand(userId, orderId)
        );

        assertThat(result).isEqualTo(PaymentPrepareResult.from(pendingPayment));
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("실패한 결제만 있으면 새로운 결제를 생성한다")
    void prepare_failedPayments_savesNewPayment() {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment firstFailedPayment = Payment.create(orderId, userId, amount);
        Payment secondFailedPayment = Payment.create(orderId, userId, amount);
        firstFailedPayment.fail();
        secondFailedPayment.fail();
        given(orderQueryPort.getPayableOrder(orderId, userId))
                .willReturn(new PayableOrder(orderId, userId, amount));
        given(paymentRepository.findAllByOrderId(orderId))
                .willReturn(List.of(firstFailedPayment, secondFailedPayment));

        PaymentPrepareResult result = paymentService.prepare(new PaymentPrepareCommand(userId, orderId));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepository).should().save(captor.capture());
        Payment newPayment = captor.getValue();
        assertThat(newPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(newPayment.getPaymentId()).isNotIn(
                firstFailedPayment.getPaymentId(),
                secondFailedPayment.getPaymentId()
        );
        assertThat(result).isEqualTo(PaymentPrepareResult.from(newPayment));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "CANCELED", "REFUNDED"})
    @DisplayName("처리 완료된 결제가 있으면 재결제를 거부한다")
    void prepare_completedPayment_throwsRetryNotAllowed(PaymentStatus status) {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment payment = paymentWithStatus(orderId, userId, amount, status);
        given(orderQueryPort.getPayableOrder(orderId, userId))
                .willReturn(new PayableOrder(orderId, userId, amount));
        given(paymentRepository.findAllByOrderId(orderId)).willReturn(List.of(payment));

        assertPaymentError(
                () -> paymentService.prepare(new PaymentPrepareCommand(userId, orderId)),
                PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED
        );

        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "CANCELED", "REFUNDED"})
    @DisplayName("실패 이력과 처리 완료된 결제가 함께 있으면 재결제를 거부한다")
    void prepare_failedAndCompletedPayments_throwsRetryNotAllowed(PaymentStatus status) {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment failedPayment = Payment.create(orderId, userId, amount);
        failedPayment.fail();
        Payment completedPayment = paymentWithStatus(orderId, userId, amount, status);
        given(orderQueryPort.getPayableOrder(orderId, userId))
                .willReturn(new PayableOrder(orderId, userId, amount));
        given(paymentRepository.findAllByOrderId(orderId))
                .willReturn(List.of(failedPayment, completedPayment));

        assertPaymentError(
                () -> paymentService.prepare(new PaymentPrepareCommand(userId, orderId)),
                PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED
        );

        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 승인 성공 시 PaymentCompletedEvent를 발행해야 한다")
    void confirm_PublishPaymentCompletedEvent_Success() {
        // given
        String orderId = "order-1";
        Long userId = 1L;
        String paymentKey = "payment-key";
        BigDecimal amount = BigDecimal.valueOf(50000);
        Payment payment = Payment.create(orderId, userId, amount);

        given(paymentRepository.findById(payment.getPaymentId())).willReturn(Optional.of(payment));

        // when
        paymentService.confirm(new PaymentConfirmCommand(
                userId,
                payment.getPaymentId(),
                orderId,
                paymentKey,
                amount
        ));

        // then
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publish(eventCaptor.capture());

        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(event.paymentId()).isEqualTo(payment.getPaymentId());
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.approvedAmount()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("승인할 결제가 없으면 결제 없음 예외가 발생하고 이벤트를 발행하지 않는다")
    void confirm_missingPayment_throwsPaymentNotFoundWithoutEvent() {
        String paymentId = "missing-payment";
        String orderId = "order-1";
        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
                        paymentId,
                        orderId,
                        "payment-key",
                        BigDecimal.valueOf(50_000)
                )),
                PaymentErrorCode.PAYMENT_NOT_FOUND
        );

        then(paymentRepository).should(never()).save(any(Payment.class));
        then(eventPublisher).should(never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("다른 사용자가 결제를 승인하면 저장하거나 이벤트를 발행하지 않는다")
    void confirm_differentUser_doesNotSaveOrPublishEvent() {
        String orderId = "order-1";
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment payment = Payment.create(orderId, 1L, amount);
        given(paymentRepository.findById(payment.getPaymentId())).willReturn(Optional.of(payment));

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        2L,
                        payment.getPaymentId(),
                        orderId,
                        "payment-key",
                        amount
                )),
                PaymentErrorCode.PAYMENT_ACCESS_DENIED
        );

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentKey()).isNull();
        assertThat(payment.getApprovedAt()).isNull();
        then(paymentRepository).should(never()).save(any(Payment.class));
        then(eventPublisher).should(never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("결제의 주문과 요청 주문이 다르면 저장하거나 이벤트를 발행하지 않는다")
    void confirm_differentOrder_doesNotSaveOrPublishEvent() {
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment payment = Payment.create("order-1", 1L, amount);
        given(paymentRepository.findById(payment.getPaymentId())).willReturn(Optional.of(payment));

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
                        payment.getPaymentId(),
                        "order-2",
                        "payment-key",
                        amount
                )),
                PaymentErrorCode.PAYMENT_ORDER_MISMATCH
        );

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentKey()).isNull();
        then(paymentRepository).should(never()).save(any(Payment.class));
        then(eventPublisher).should(never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("승인 금액이 결제 금액과 다르면 저장하거나 이벤트를 발행하지 않는다")
    void confirm_mismatchedAmount_doesNotSaveOrPublishEvent() {
        String orderId = "order-1";
        Payment payment = Payment.create(
                orderId,
                1L,
                BigDecimal.valueOf(50_000)
        );
        given(paymentRepository.findById(payment.getPaymentId())).willReturn(Optional.of(payment));

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
                        payment.getPaymentId(),
                        orderId,
                        "payment-key",
                        BigDecimal.valueOf(49_000)
                )),
                PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH
        );

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentKey()).isNull();
        then(paymentRepository).should(never()).save(any(Payment.class));
        then(eventPublisher).should(never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("이미 승인된 결제를 다시 승인하면 저장하거나 이벤트를 발행하지 않는다")
    void confirm_approvedPayment_doesNotSaveOrPublishEventAgain() {
        String orderId = "order-1";
        BigDecimal amount = BigDecimal.valueOf(50_000);
        Payment payment = Payment.create(orderId, 1L, amount);
        payment.approve("payment-key-1", amount);
        given(paymentRepository.findById(payment.getPaymentId())).willReturn(Optional.of(payment));

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
                        payment.getPaymentId(),
                        orderId,
                        "payment-key-2",
                        amount
                )),
                PaymentErrorCode.PAYMENT_NOT_PENDING
        );

        assertThat(payment.getPaymentKey()).isEqualTo("payment-key-1");
        then(paymentRepository).should(never()).save(any(Payment.class));
        then(eventPublisher).should(never()).publish(any(PaymentCompletedEvent.class));
    }

    private Payment paymentWithStatus(
            String orderId,
            Long userId,
            BigDecimal amount,
            PaymentStatus status
    ) {
        Payment payment = Payment.create(orderId, userId, amount);
        payment.approve("payment-key", amount);
        if (status == PaymentStatus.CANCELED) {
            payment.cancel();
        }
        if (status == PaymentStatus.REFUNDED) {
            payment.refund();
        }
        return payment;
    }

    private void assertPaymentError(Runnable action, PaymentErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
