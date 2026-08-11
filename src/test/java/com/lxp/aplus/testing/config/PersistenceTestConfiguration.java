package com.lxp.aplus.testing.config;

import com.lxp.aplus.cart.adapter.out.persistence.CartJpaRepository;
import com.lxp.aplus.cart.domain.Cart;
import com.lxp.aplus.order.adapter.out.persistence.OrderJpaRepository;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.payment.adapter.out.persistence.PaymentJpaRepository;
import com.lxp.aplus.payment.adapter.out.persistence.IdempotencyRecordJpaRepository;
import com.lxp.aplus.payment.domain.IdempotencyRecord;
import com.lxp.aplus.payment.domain.Payment;
import com.lxp.aplus.payment.domain.PaymentAttempt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Cart, Order, Payment 영속성 테스트에서 사용할 JPA 구성입니다.
 *
 * 기본 {@code @DataJpaTest}는 애플리케이션의 모든 엔티티와 Repository를 탐색합니다.
 * 이 프로젝트에서는 그 과정에서 테스트 대상이 아닌
 * Review의 MySQL 전용 컬럼 정의와 QueryDSL Repository 의존성까지 로드되어
 * H2 기반 테스트 컨텍스트가 시작되지 않습니다.
 *
 * 따라서 세 도메인의 엔티티와 Spring Data JPA Repository만 명시적으로 등록하여 테스트 범위를 격리하고,
 * 각 테스트가 해당 도메인의 매핑, cascade, 조회 및 DB 제약 조건에만 집중하도록 합니다.
 */
@TestConfiguration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = {
        Cart.class, Order.class, Payment.class, PaymentAttempt.class, IdempotencyRecord.class
})
@EnableJpaRepositories(basePackageClasses = {
        CartJpaRepository.class,
        OrderJpaRepository.class,
        PaymentJpaRepository.class,
        IdempotencyRecordJpaRepository.class
})
public class PersistenceTestConfiguration {
}
