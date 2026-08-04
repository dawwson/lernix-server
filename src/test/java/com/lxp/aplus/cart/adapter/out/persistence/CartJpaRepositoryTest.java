package com.lxp.aplus.cart.adapter.out.persistence;

import com.lxp.aplus.cart.domain.Cart;
import com.lxp.aplus.cart.domain.CartItem;
import com.lxp.aplus.testing.config.PersistenceTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PersistenceTestConfiguration.class)
@DisplayName("CartJpaRepository 테스트")
class CartJpaRepositoryTest {

    @Autowired
    private CartJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("장바구니를 저장하면 항목도 함께 저장되고 사용자 ID로 조회할 수 있다")
    void save_cartWithItems_persistsAggregateAndFindsByUserId() {
        Cart cart = Cart.create(1L);
        cart.addCartItem(10L);
        cart.addCartItem(20L);

        repository.save(cart);
        flushAndClear();

        Optional<Cart> result = repository.findByUserId(1L);
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getCourseIds()).containsExactly(10L, 20L);
        assertThat(result.orElseThrow().getCartItems())
                .extracting(CartItem::getId)
                .doesNotContainNull();
    }

    @Test
    @DisplayName("장바구니 항목을 제거하면 DB에서도 해당 항목이 삭제된다")
    void removeCartItem_persistedItem_removesOrphanFromDatabase() {
        Cart cart = Cart.create(1L);
        cart.addCartItem(10L);
        repository.save(cart);
        flushAndClear();

        Cart savedCart = repository.findByUserId(1L).orElseThrow();
        Long cartItemId = savedCart.getCartItems().get(0).getId();
        savedCart.removeCartItem(cartItemId);
        flushAndClear();

        Cart result = repository.findByUserId(1L).orElseThrow();
        Long cartItemCount = entityManager.createQuery(
                        "select count(cartItem) from CartItem cartItem",
                        Long.class
                )
                .getSingleResult();
        assertThat(result.getCartItems()).isEmpty();
        assertThat(cartItemCount).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회하면 빈 결과를 반환한다")
    void findByUserId_missingUser_returnsEmpty() {
        assertThat(repository.findByUserId(999L)).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
