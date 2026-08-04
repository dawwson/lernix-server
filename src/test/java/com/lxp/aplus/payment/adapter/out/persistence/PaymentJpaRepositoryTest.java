package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentStatus;
import com.lxp.aplus.support.PersistenceTestConfiguration;
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

    @Autowired
    private PaymentJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("결제를 저장하면 주문 ID로 조회할 수 있다")
    void save_payment_findsByOrderId() {
        Payment payment = Payment.create(
                "order-1",
                1L,
                BigDecimal.valueOf(40_000)
        );

        repository.save(payment);
        flushAndClear();

        Payment result = repository.findByOrderId("order-1").orElseThrow();
        assertThat(result.getPaymentId()).isEqualTo(payment.getPaymentId());
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo("40000");
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("결제를 승인하면 상태와 결제 키가 DB에 저장된다")
    void approve_persistedPayment_updatesPaymentState() {
        BigDecimal amount = BigDecimal.valueOf(40_000);
        Payment payment = Payment.create("order-1", 1L, amount);
        repository.save(payment);
        flushAndClear();

        Payment savedPayment = repository.findByOrderId("order-1").orElseThrow();
        savedPayment.approve("payment-key", amount);
        flushAndClear();

        Payment result = repository.findByOrderId("order-1").orElseThrow();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getPaymentKey()).isEqualTo("payment-key");
        assertThat(result.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID로 결제를 조회하면 빈 결과를 반환한다")
    void findByOrderId_missingOrder_returnsEmpty() {
        assertThat(repository.findByOrderId("missing-order")).isEmpty();
    }

    @Test
    @DisplayName("같은 결제 키를 저장하면 DB 제약 조건 위반이 발생한다")
    void save_duplicatePaymentKey_throwsDataIntegrityViolation() {
        BigDecimal amount = BigDecimal.valueOf(40_000);
        Payment firstPayment = Payment.create("order-1", 1L, amount);
        firstPayment.approve("duplicated-key", amount);
        repository.saveAndFlush(firstPayment);

        Payment secondPayment = Payment.create("order-2", 1L, amount);
        secondPayment.approve("duplicated-key", amount);

        assertThatThrownBy(() -> repository.saveAndFlush(secondPayment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
