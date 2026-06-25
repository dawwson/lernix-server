package com.lxp.aplus.cart.domain;

import java.util.Optional;

public interface CartRepository {

    Optional<Cart> findByUserId(Long userId);

    Cart save(Cart cart);
}
