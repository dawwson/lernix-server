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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
        assertThat(result).isEqualTo(new PaymentPrepareResult(orderId, amount));
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

        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

        // when
        paymentService.confirm(new PaymentConfirmCommand(userId, orderId, paymentKey, amount));

        // then
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publish(eventCaptor.capture());

        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.paymentId()).isEqualTo(payment.getPaymentId());
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.approvedAmount()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("승인할 결제가 없으면 결제 없음 예외가 발생하고 이벤트를 발행하지 않는다")
    void confirm_missingPayment_throwsPaymentNotFoundWithoutEvent() {
        String orderId = "missing-order";
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
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
    @DisplayName("승인 금액이 결제 금액과 다르면 저장하거나 이벤트를 발행하지 않는다")
    void confirm_mismatchedAmount_doesNotSaveOrPublishEvent() {
        String orderId = "order-1";
        Payment payment = Payment.create(
                orderId,
                1L,
                BigDecimal.valueOf(50_000)
        );
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
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
        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

        assertPaymentError(
                () -> paymentService.confirm(new PaymentConfirmCommand(
                        1L,
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

    private void assertPaymentError(Runnable action, PaymentErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
