package com.lxp.aplus.payment.domain;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 도메인 테스트")
class PaymentTest {

    @Test
    @DisplayName("결제를 생성하면 주문 정보와 초기 상태가 설정된다")
    void create_validOrder_initializesPayment() {
        Payment payment = payment(40_000);

        assertThat(payment.getPaymentId()).isNotBlank();
        assertThat(payment.getOrderId()).isEqualTo("order-1");
        assertThat(payment.getUserId()).isEqualTo(1L);
        assertThat(payment.getAmount()).isEqualByComparingTo("40000");
        assertThat(payment.getCurrency()).isEqualTo("KRW");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getPgProvider()).isEqualTo(PgProvider.TOSS);
    }

    @Test
    @DisplayName("주문 ID 없이 결제를 생성하면 예외가 발생한다")
    void create_nullOrderId_throwsInvalidOrderId() {
        assertPaymentError(
                () -> Payment.create(null, 1L, BigDecimal.valueOf(40_000)),
                PaymentErrorCode.PAYMENT_INVALID_ORDER_ID
        );
    }

    @Test
    @DisplayName("결제 소유자가 접근하면 권한 검증을 통과한다")
    void validateOwner_owner_completesNormally() {
        Payment payment = payment(40_000);

        assertThatCode(() -> payment.validateOwner(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("다른 사용자가 접근하면 권한 예외가 발생하고 결제 상태를 유지한다")
    void validateOwner_differentUser_throwsAccessDeniedAndKeepsState() {
        Payment payment = payment(40_000);

        assertPaymentError(
                () -> payment.validateOwner(2L),
                PaymentErrorCode.PAYMENT_ACCESS_DENIED
        );

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentKey()).isNull();
        assertThat(payment.getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("대기 중인 결제를 승인하면 승인 정보가 저장된다")
    void approve_pendingPayment_approvesPayment() {
        Payment payment = payment(40_000);

        payment.approve("payment-key", BigDecimal.valueOf(40_000));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("승인 금액이 결제 금액과 다르면 결제 상태를 변경하지 않는다")
    void approve_mismatchedAmount_keepsPaymentPending() {
        Payment payment = payment(40_000);

        assertPaymentError(
                () -> payment.approve("payment-key", BigDecimal.valueOf(39_000)),
                PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH
        );
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentKey()).isNull();
        assertThat(payment.getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("대기 상태가 아닌 결제를 승인하면 예외가 발생한다")
    void approve_failedPayment_throwsNotPending() {
        Payment payment = payment(40_000);
        payment.fail();

        assertPaymentError(
                () -> payment.approve("payment-key", BigDecimal.valueOf(40_000)),
                PaymentErrorCode.PAYMENT_NOT_PENDING
        );
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaymentKey()).isNull();
    }

    @Test
    @DisplayName("대기 중인 결제를 실패 처리하면 실패 상태가 된다")
    void fail_pendingPayment_marksPaymentFailed() {
        Payment payment = payment(40_000);

        payment.fail();

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("승인된 결제를 취소하면 취소 상태와 시간이 저장된다")
    void cancel_approvedPayment_cancelsPayment() {
        Payment payment = approvedPayment();

        payment.cancel();

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("승인 전 결제를 취소하면 예외가 발생한다")
    void cancel_pendingPayment_throwsNotApproved() {
        Payment payment = payment(40_000);

        assertPaymentError(payment::cancel, PaymentErrorCode.PAYMENT_NOT_APPROVED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCanceledAt()).isNull();
    }

    @Test
    @DisplayName("승인된 결제를 환불하면 환불 상태와 시간이 저장된다")
    void refund_approvedPayment_refundsPayment() {
        Payment payment = approvedPayment();

        payment.refund();

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isNotNull();
    }

    @Test
    @DisplayName("승인 전 결제를 환불하면 예외가 발생한다")
    void refund_pendingPayment_throwsNotApproved() {
        Payment payment = payment(40_000);

        assertPaymentError(payment::refund, PaymentErrorCode.PAYMENT_NOT_APPROVED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getRefundedAt()).isNull();
    }

    private Payment payment(int amount) {
        return Payment.create("order-1", 1L, BigDecimal.valueOf(amount));
    }

    private Payment approvedPayment() {
        Payment payment = payment(40_000);
        payment.approve("payment-key", BigDecimal.valueOf(40_000));
        return payment;
    }

    private void assertPaymentError(Runnable action, PaymentErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
