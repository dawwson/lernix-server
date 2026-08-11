package com.lxp.aplus.order.adapter.out.persistence;

import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderItem;
import com.lxp.aplus.testing.config.PersistenceTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PersistenceTestConfiguration.class)
@DisplayName("OrderJpaRepository 테스트")
class OrderJpaRepositoryTest {

    @Autowired
    private OrderJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("주문을 저장하면 주문 항목도 함께 저장된다")
    void save_orderWithItems_persistsAggregate() {
        Order order = Order.create(
                1L,
                List.of(
                        OrderItem.createCourseItem(10L, BigDecimal.valueOf(10_000)),
                        OrderItem.createCourseItem(20L, BigDecimal.valueOf(30_000))
                )
        );

        repository.save(order);
        flushAndClear();

        Order result = repository.findById(order.getOrderId()).orElseThrow();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo("40000");
        assertThat(result.getOrderStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(result.getOrderItems()).hasSize(2);
        assertThat(result.getOrderItems())
                .extracting(OrderItem::getId)
                .doesNotContainNull();
        assertThat(result.getOrderItems())
                .extracting(OrderItem::getItemId)
                .containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("주문을 완료하면 상태와 승인 결제 정보가 DB에 저장된다")
    void completeWithApprovedPayment_persistedOrder_updatesOrderState() {
        BigDecimal amount = BigDecimal.valueOf(40_000);
        Order order = Order.create(
                1L,
                List.of(OrderItem.createCourseItem(10L, amount))
        );
        repository.save(order);
        flushAndClear();

        Order savedOrder = repository.findById(order.getOrderId()).orElseThrow();
        savedOrder.completeWithApprovedPayment("payment-1", amount);
        flushAndClear();

        Order result = repository.findById(order.getOrderId()).orElseThrow();
        assertThat(result.getOrderStatus()).isEqualTo(Order.Status.COMPLETED);
        assertThat(result.getApprovedPaymentId()).isEqualTo("payment-1");
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("주문 완료 처리용 비관적 잠금 조회로 주문을 찾는다")
    void findByIdForUpdate_existingOrder_returnsOrder() {
        Order order = Order.create(
                1L,
                List.of(OrderItem.createCourseItem(10L, BigDecimal.valueOf(40_000)))
        );
        repository.save(order);
        flushAndClear();

        Order result = repository.findByIdForUpdate(order.getOrderId()).orElseThrow();

        assertThat(result.getOrderId()).isEqualTo(order.getOrderId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
