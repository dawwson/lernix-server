package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
import com.lxp.aplus.testing.config.PersistenceTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PersistenceTestConfiguration.class)
@DisplayName("PaymentJpaRepository 테스트")
class PaymentJpaRepositoryTest {
    @Autowired PaymentJpaRepository paymentRepository;
    @Autowired EntityManager entityManager;

    @Test
    @DisplayName("Payment를 저장하면 현재 PaymentAttempt도 함께 저장된다")
    void save_paymentWithAttempt_cascadesAttempt() {
        Payment payment = Payment.create("order-1", 1L, BigDecimal.valueOf(40_000));
        PaymentAttempt attempt = payment.prepareAttempt();
        paymentRepository.save(payment);
        flushAndClear();

        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        PaymentAttempt savedAttempt = savedPayment.getPendingAttempt().orElseThrow();
        assertThat(savedAttempt.getId()).isEqualTo(attempt.getId());
        assertThat(savedPayment.getStatus()).isEqualTo(Payment.Status.UNPAID);
        assertThat(savedAttempt.getStatus()).isEqualTo(PaymentAttempt.Status.PENDING);
    }

    @Test
    @DisplayName("주문 ID로 잠금 조회하면 Payment와 PaymentAttempt를 함께 반환한다")
    void findByOrderIdForUpdate_existingPayment_returnsAggregate() {
        Payment payment = Payment.create("order-1", 1L, BigDecimal.valueOf(40_000));
        PaymentAttempt attempt = payment.prepareAttempt();
        paymentRepository.saveAndFlush(payment);
        flushAndClear();

        Payment result = paymentRepository.findByOrderIdForUpdate("order-1").orElseThrow();

        assertThat(result.getId()).isEqualTo(payment.getId());
        assertThat(result.getAttempts())
                .extracting(PaymentAttempt::getId)
                .containsExactly(attempt.getId());
    }

    @Test
    @DisplayName("동일 주문에는 Payment를 하나만 저장할 수 있다")
    void save_duplicateOrderId_throwsDataIntegrityViolation() {
        paymentRepository.saveAndFlush(Payment.create("order-1", 1L, BigDecimal.TEN));
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(Payment.create("order-1", 1L, BigDecimal.TEN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("paymentKey는 모든 PaymentAttempt에서 유일해야 한다")
    void save_duplicatePaymentKey_throwsDataIntegrityViolation() {
        Payment first = Payment.create("order-1", 1L, BigDecimal.TEN);
        PaymentAttempt firstAttempt = first.prepareAttempt();
        first.approve(firstAttempt.getId(), "duplicated-key", BigDecimal.TEN);
        paymentRepository.saveAndFlush(first);

        Payment second = Payment.create("order-2", 1L, BigDecimal.TEN);
        PaymentAttempt secondAttempt = second.prepareAttempt();
        second.approve(secondAttempt.getId(), "duplicated-key", BigDecimal.TEN);
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void flushAndClear() { entityManager.flush(); entityManager.clear(); }
}
