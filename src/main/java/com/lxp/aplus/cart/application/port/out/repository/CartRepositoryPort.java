package com.lxp.aplus.cart.application.port.out.repository;

import com.lxp.aplus.cart.domain.Cart;

import java.util.Optional;

public interface CartRepositoryPort {

    Optional<Cart> findByUserId(Long userId);

    Cart save(Cart cart);
}
