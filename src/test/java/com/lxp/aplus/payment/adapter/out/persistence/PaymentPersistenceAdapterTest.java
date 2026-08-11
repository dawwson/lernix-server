package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.domain.Payment;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentPersistenceAdapter 단위 테스트")
class PaymentPersistenceAdapterTest {

    @Mock
    private PaymentJpaRepository jpaRepository;

    @InjectMocks
    private PaymentPersistenceAdapter adapter;

    @Test
    @DisplayName("동일 주문의 Payment 생성이 충돌하면 결제 준비 충돌 예외로 변환한다")
    void save_duplicateOrderId_throwsPaymentPrepareConflict() {
        Payment payment = Payment.create("order-1", 1L, BigDecimal.valueOf(40_000));
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate order", new SQLException(), "uk_payments_order_id");
        given(jpaRepository.saveAndFlush(payment))
                .willThrow(new DataIntegrityViolationException("duplicate order", constraintViolation));

        assertThatThrownBy(() -> adapter.save(payment))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_PREPARE_CONFLICT);
    }
}
