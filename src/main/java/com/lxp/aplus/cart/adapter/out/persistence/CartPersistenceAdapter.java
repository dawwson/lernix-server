package com.lxp.aplus.cart.adapter.out.persistence;

import com.lxp.aplus.cart.application.port.out.repository.CartRepositoryPort;
import com.lxp.aplus.cart.domain.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartPersistenceAdapter implements CartRepositoryPort {

    private final CartJpaRepository jpaRepository;

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public Cart save(Cart cart) {
        return jpaRepository.save(cart);
    }
}
