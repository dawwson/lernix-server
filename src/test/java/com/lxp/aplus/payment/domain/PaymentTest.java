package com.lxp.aplus.payment.domain;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 도메인 테스트")
class PaymentTest {

    @Test
    @DisplayName("결제는 미결제 상태로 생성된다")
    void create_validOrder_returnsUnpaidPayment() {
        Payment payment = payment();

        assertThat(payment.getId()).isNotBlank();
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.UNPAID);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("새 결제 시도를 준비하면 PENDING 시도가 생성된다")
    void prepareAttempt_noPendingAttempt_createsPendingAttempt() {
        Payment payment = payment();

        PaymentAttempt attempt = payment.prepareAttempt();

        assertThat(attempt.getPaymentId()).isEqualTo(payment.getId());
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttempt.Status.PENDING);
        assertThat(payment.getPendingAttempt()).contains(attempt);
    }

    @Test
    @DisplayName("PENDING 시도가 있으면 새로운 결제 시도를 거부한다")
    void prepareAttempt_pendingAttempt_throwsRetryNotAllowed() {
        Payment payment = payment();
        payment.prepareAttempt();

        assertThatThrownBy(payment::prepareAttempt)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_RETRY_NOT_ALLOWED);
    }

    @Test
    @DisplayName("현재 시도를 승인하면 결제와 시도가 함께 완료된다")
    void approve_currentPendingAttempt_completesPayment() {
        Payment payment = payment();
        PaymentAttempt attempt = payment.prepareAttempt();

        payment.approve(attempt.getId(), "payment-key", BigDecimal.valueOf(40_000));

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttempt.Status.APPROVED);
        assertThat(attempt.getPaymentKey()).isEqualTo("payment-key");
    }

    @Test
    @DisplayName("시도가 실패해도 결제는 미결제 상태를 유지한다")
    void fail_pendingAttempt_keepsPaymentUnpaid() {
        Payment payment = payment();
        PaymentAttempt attempt = payment.prepareAttempt();

        payment.fail(attempt);

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.UNPAID);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttempt.Status.FAILED);
        assertThat(attempt.getFailedAt()).isNotNull();
        assertThat(payment.getPendingAttempt()).isEmpty();
    }

    @Test
    @DisplayName("현재 시도가 아닌 이전 시도는 승인할 수 없다")
    void approve_previousAttempt_throwsBusinessException() {
        Payment payment = payment();
        PaymentAttempt oldAttempt = payment.prepareAttempt();
        payment.fail(oldAttempt);
        payment.prepareAttempt();

        assertThatThrownBy(() -> payment.approve(oldAttempt.getId(), "key", BigDecimal.valueOf(40_000)))
                .isInstanceOf(BusinessException.class);
    }

    private Payment payment() {
        return Payment.create("order-1", 1L, BigDecimal.valueOf(40_000));
    }
}
