package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.application.port.out.repository.PaymentRepositoryPort;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
            // NOTE: saveAndFlush - SQL을 즉시 실행해 Payment 관련 UNIQUE 무결성 위반을 현재 호출 안에서 확인한다.
            return jpaRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            // TODO: application layer에서 예외 처리를 할 수 있을지 고민 필요
            if (hasUniqueConstraint(exception, "uk_payments_order_id")) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_PREPARE_CONFLICT);
            }
            if (hasUniqueConstraint(exception, "uk_payment_attempts_payment_key")) {
                throw new BusinessException(PaymentErrorCode.PAYMENT_APPROVAL_CONFLICT);
            }
            throw exception;
        }
    }

    @Override
    public Optional<Payment> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(String id) {
        Optional<Payment> result = jpaRepository.findByIdForUpdate(id);
        result.ifPresent(payment -> Hibernate.initialize(payment.getAttempts()));
        return result;
    }

    @Override
    public Optional<Payment> findByOrderIdForUpdate(String orderId) {
        Optional<Payment> result = jpaRepository.findByOrderIdForUpdate(orderId);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        Payment payment = result.get();

        // NOTE: Payment root의 잠금을 획득한 뒤 Attempt를 조회해야
        //  잠금 대기 중 다른 트랜잭션이 추가한 최신 Attempt까지 확인할 수 있다.
        Hibernate.initialize(payment.getAttempts());

        return Optional.of(payment);
    }

    @Override
    public Optional<Payment> findByAttemptId(String attemptId) {
        return jpaRepository.findByAttemptId(attemptId);
    }

    private boolean hasUniqueConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                return constraintViolation.getConstraintName()
                        .toLowerCase()
                        .contains(constraintName.toLowerCase());
            }
            cause = cause.getCause();
        }
        return false;
    }

}
