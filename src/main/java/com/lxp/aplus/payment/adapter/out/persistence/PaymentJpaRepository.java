package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, String>  {
    @Override
    // NOTE: @EntityGraph - Payment 조회 시 attempts를 함께 fetch하여 추가 지연 로딩 쿼리를 방지한다.
    @EntityGraph(attributePaths = "attempts")
    Optional<Payment> findById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // NOTE: custom method name 사용을 위해 쿼리 추가
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    @EntityGraph(attributePaths = "attempts")
    @Query("select p from Payment p where exists " +
            "(select a.id from PaymentAttempt a where a.payment = p and a.id = :attemptId)")
    Optional<Payment> findByAttemptId(@Param("attemptId") String attemptId);
}
