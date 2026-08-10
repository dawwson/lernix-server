package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.domain.IdempotencyRecord;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyRecordPersistenceAdapter 단위 테스트")
class IdempotencyRecordPersistenceAdapterTest {

    @Mock
    private IdempotencyRecordJpaRepository jpaRepository;

    @InjectMocks
    private IdempotencyRecordPersistenceAdapter adapter;

    @Test
    @DisplayName("동일 사용자의 멱등성 키 생성이 충돌하면 처리 중 예외로 변환한다")
    void save_duplicateUserAndKey_throwsRequestInProgress() {
        IdempotencyRecord record = IdempotencyRecord.create(1L, "same-key", "request-hash");
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate key", new SQLException(), "PRIMARY");
        given(jpaRepository.saveAndFlush(record))
                .willThrow(new DataIntegrityViolationException("duplicate key", constraintViolation));

        assertThatThrownBy(() -> adapter.save(record))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_IDEMPOTENCY_REQUEST_IN_PROGRESS);
    }
}
