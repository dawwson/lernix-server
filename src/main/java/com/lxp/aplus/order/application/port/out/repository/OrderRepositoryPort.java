package com.lxp.aplus.order.application.port.out.repository;

import com.lxp.aplus.order.domain.Order;

import java.util.Optional;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(String id);

    Optional<Order> findByIdForUpdate(String id);
}
