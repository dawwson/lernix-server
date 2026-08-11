package com.lxp.aplus.order.domain;

import com.lxp.aplus.common.domain.BaseAggregateRoot;
import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// TODO: amount, currency 묶어서 VO(Money)로 만들기
// TODO: 특정 상태에 종속적인 필드를 관리하는 구조적인 방법 고민(approvedPaymentId, cancelReason)
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseAggregateRoot {

    @Id
    @Column(name = "id")
    private String orderId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private String currency;

    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Getter(AccessLevel.NONE) // Lombok getter 생성을 비활성화
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'PENDING'")
    private Status orderStatus = Status.PENDING;

    @Column
    private String approvedPaymentId; // orderStatus = COMPLETED일 때만 존재

    @Column
    private String cancelReason;

    @Column
    private LocalDateTime completedAt;

    private Order(
            String orderId,
            Long userId,
            BigDecimal amount,
            List<OrderItem> orderItems
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.currency = "KRW";
        this.amount = amount;

        for (OrderItem orderItem : orderItems) {
            orderItem.assignOrder(this);
            this.orderItems.add(orderItem);
        }
    }

    /*
     * 주문 생성
     * - Order는 항상 PENDING 상태로만 생성된다.
     * - OrderLine의 price 합계와 금액이 일치해야 한다.
     */
    public static Order create(
            Long userId,
            List<OrderItem> orderItems
    ) {
        validateUserId(userId);
        validateOrderItems(orderItems);

        // 1. orderId 생성
        String orderId = UUID.randomUUID().toString();

        // 2. 총액 계산
        BigDecimal amount = orderItems.stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Order 생성 및 반환 (생성자를 통해 불변성 확보)
        return new Order(
                orderId,
                userId,
                amount,
                new ArrayList<>(orderItems)
        );
    }

    /*
     * 결제 승인 -> 주문 완료
     * - PENDING 상태에서만 수행 가능
     * - 하나의 Order에는 승인된 Payment가 하나만 존재
     * - 승인된 결제 금액 = 주문 금액
     *
     */
    public CompletionResult completeWithApprovedPayment(
            String approvedPaymentId,
            BigDecimal approvedAmount
    ) {
        validateApprovedPaymentId(approvedPaymentId);

        if (this.orderStatus == Status.COMPLETED) {
            validateSameApprovedPayment(approvedPaymentId);
            validateAmount(approvedAmount);
            return CompletionResult.ALREADY_COMPLETED;
        }

        validatePending();
        validateApprovedPaymentNotAssigned();
        validateAmount(approvedAmount);

        this.orderStatus = Status.COMPLETED;
        this.approvedPaymentId = approvedPaymentId;
        this.completedAt = LocalDateTime.now();

        return CompletionResult.FIRST_COMPLETION;
    }

    /*
     * 주문 취소
     * - PENDING 상태에서만 취소 가능
     * - COMPLETED → CANCELED로 직접 변경 불가 (= Payment BC의 책임)
     */
    public void cancel(String reason) {
        validatePending();
        validateCancelReason(reason);
        this.orderStatus = Status.CANCELED;
        this.cancelReason = reason;
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_USER);
        }
    }

    private static void validateOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_ITEM);
        }

        Set<OrderItem> uniqueItems = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean hasInvalidItem = orderItems.stream()
                .anyMatch(item -> item == null || !uniqueItems.add(item));

        if (hasInvalidItem) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_ITEM);
        }

        if (orderItems.stream().anyMatch(item -> item.getOrder() != null)) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEM_ALREADY_ASSIGNED);
        }
    }

    private void validatePending() {
        if (this.orderStatus != Status.PENDING) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_STATUS);
        }
    }

    private void validateApprovedPaymentId(String approvedPaymentId) {
        if (approvedPaymentId == null || approvedPaymentId.isBlank()) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_PAYMENT_INFO);
        }
    }

    private void validateApprovedPaymentNotAssigned() {
        if (this.approvedPaymentId != null) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_PAID);
        }
    }

    private void validateSameApprovedPayment(String paymentId) {
        if (!Objects.equals(this.approvedPaymentId, paymentId)) {
            throw new BusinessException(OrderErrorCode.ORDER_PAYMENT_CONFLICT);
        }
    }

    private void validateAmount(BigDecimal approvedAmount) {
        if (approvedAmount == null) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_PAYMENT_INFO);
        }

        boolean isAmountMismatched = this.amount.compareTo(approvedAmount) != 0;

        if (isAmountMismatched) {
            throw new BusinessException(OrderErrorCode.ORDER_AMOUNT_MISMATCH);
        }
    }

    private void validateCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_REASON_REQUIRED);
        }
    }

    public enum Status {
        PENDING,
        COMPLETED,
        CANCELED
    }

    public enum CompletionResult { FIRST_COMPLETION, ALREADY_COMPLETED }
}
