package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.application.port.out.repository.PaymentRepositoryPort;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        try {
            // NOTE: SQL 즉시 실행으로 order_id UNIQUE 무결성 위반 확인
            return jpaRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            // TODO: DB 제약 위반은 Payment 전용 예외로 변환하고, BusinessException 매핑은 application service로 이동한다.
            if (hasConstraint(exception, "uk_payments_order_id")) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_PREPARE_CONFLICT);
            }
            throw exception;
        }
    }

    @Override
    public Optional<Payment> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByOrderIdForUpdate(String orderId) {
        return jpaRepository.findByOrderIdForUpdate(orderId);
    }

    @Override
    public Optional<Payment> findByAttemptId(String attemptId) {
        return jpaRepository.findByAttemptId(attemptId);
    }

    private boolean hasConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && constraintName.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}
