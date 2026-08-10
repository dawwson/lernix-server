package com.lxp.aplus.payment.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.out.event.PaymentEventPublisherPort;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
import com.lxp.aplus.payment.application.port.out.repository.PaymentRepositoryPort;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceUnitTest {
    @Mock PaymentRepositoryPort paymentRepository;
    @Mock PaymentOrderQueryPort orderQueryPort;
    @Mock PaymentEventPublisherPort eventPublisher;
    @InjectMocks PaymentService service;

    @Test
    @DisplayName("최초 결제 준비는 Payment와 PaymentAttempt를 생성한다")
    void prepare_missingPayment_createsPaymentAndAttempt() {
        given(orderQueryPort.getPayableOrder("order-1", 1L))
                .willReturn(new PayableOrder("order-1", 1L, amount()));
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.empty());

        var result = service.prepare(new PaymentPrepareCommand(1L, "order-1"));

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepository).should().save(payment.capture());
        assertThat(payment.getValue().getPendingAttempt())
                .get().extracting(PaymentAttempt::getId)
                .isEqualTo(result.paymentAttemptId());
        assertThat(result.paymentId()).isEqualTo(payment.getValue().getId());
    }

    @Test
    @DisplayName("대기 중인 시도가 있으면 기존 시도를 반환한다")
    void prepare_pendingAttempt_returnsExistingAttempt() {
        Payment payment = Payment.create("order-1", 1L, amount());
        PaymentAttempt attempt = payment.prepareAttempt();
        given(orderQueryPort.getPayableOrder("order-1", 1L))
                .willReturn(new PayableOrder("order-1", 1L, amount()));
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));

        var result = service.prepare(new PaymentPrepareCommand(1L, "order-1"));

        assertThat(result.paymentId()).isEqualTo(payment.getId());
        assertThat(result.paymentAttemptId()).isEqualTo(attempt.getId());
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("이전 시도가 실패했으면 새로운 시도를 생성한다")
    void prepare_failedAttempt_createsNewAttempt() {
        Payment payment = Payment.create("order-1", 1L, amount());
        PaymentAttempt failed = payment.prepareAttempt();
        payment.fail(failed);
        given(orderQueryPort.getPayableOrder("order-1", 1L))
                .willReturn(new PayableOrder("order-1", 1L, amount()));
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));

        var result = service.prepare(new PaymentPrepareCommand(1L, "order-1"));

        assertThat(result.paymentId()).isEqualTo(payment.getId());
        assertThat(result.paymentAttemptId()).isNotEqualTo(failed.getId());
    }

    @Test
    @DisplayName("승인된 결제에는 새로운 시도를 생성할 수 없다")
    void prepare_paidPayment_throwsRetryNotAllowed() {
        Payment payment = Payment.create("order-1", 1L, amount());
        PaymentAttempt attempt = payment.prepareAttempt();
        payment.approve(attempt.getId(), "key", amount());
        given(orderQueryPort.getPayableOrder("order-1", 1L))
                .willReturn(new PayableOrder("order-1", 1L, amount()));
        given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.prepare(new PaymentPrepareCommand(1L, "order-1")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);
    }

    @Test
    @DisplayName("결제 승인 시 Payment와 PaymentAttempt를 함께 완료하고 이벤트를 발행한다")
    void confirm_pendingAttempt_completesPaymentAndPublishesEvent() {
        Payment payment = Payment.create("order-1", 1L, amount());
        PaymentAttempt attempt = payment.prepareAttempt();
        given(paymentRepository.findById(payment.getId())).willReturn(Optional.of(payment));

        service.confirm(new PaymentConfirmCommand(
                1L, payment.getId(), attempt.getId(), "order-1", "key", amount()));

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttempt.Status.APPROVED);
        then(paymentRepository).should().save(payment);
        then(eventPublisher).should().publish(org.mockito.ArgumentMatchers.argThat(e -> e.paymentId().equals(payment.getId())));
    }

    private BigDecimal amount() { return BigDecimal.valueOf(40_000); }
}
