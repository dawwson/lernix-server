package com.lxp.aplus.order.adapter.out.persistence;

import com.lxp.aplus.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order,String> {
}
