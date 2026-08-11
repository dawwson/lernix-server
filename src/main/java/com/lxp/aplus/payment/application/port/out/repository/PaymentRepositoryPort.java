package com.lxp.aplus.payment.application.port.out.repository;

import com.lxp.aplus.payment.domain.Payment;

import java.util.Optional;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    Optional<Payment> findByOrderIdForUpdate(String orderId);

    Optional<Payment> findByAttemptId(String attemptId);
}
