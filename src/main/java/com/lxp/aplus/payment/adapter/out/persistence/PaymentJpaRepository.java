package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentJpaRepository extends JpaRepository<Payment, String>  {
    List<Payment> findAllByOrderId(String orderId);
}
