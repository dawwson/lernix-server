package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.application.port.out.idempotency.IdempotencyRecordStorePort;
import com.lxp.aplus.payment.domain.IdempotencyRecord;
import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyRecordPersistenceAdapter implements IdempotencyRecordStorePort {

    private final IdempotencyRecordJpaRepository jpaRepository;

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        try {
            // NOTE: SQL 즉시 실행으로 복합 PK 무결성 위반 확인
            return jpaRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            // TODO: DB 제약 위반은 Payment 전용 예외로 변환하고, BusinessException 매핑은 application service로 이동한다.
            if (hasPrimaryKeyConstraint(exception)) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_IDEMPOTENCY_REQUEST_IN_PROGRESS);
            }
            throw exception;
        }
    }

    @Override
    public Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey) {
        return jpaRepository.findById(IdempotencyRecord.Id.of(userId, idempotencyKey));
    }

    private boolean hasPrimaryKeyConstraint(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                return constraintName != null && constraintName.toUpperCase().contains("PRIMARY");
            }
            cause = cause.getCause();
        }
        return false;
    }
}
