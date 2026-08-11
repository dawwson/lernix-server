package com.lxp.aplus.order.domain;

import com.lxp.aplus.common.domain.BaseTimeEntity;
import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    // TODO: orderItemId -> id로 수정
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal price;

    protected OrderItem(ItemType itemType, Long itemId, BigDecimal price) {
        if (itemType == null || itemId == null || price == null || price.signum() < 0) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_ITEM);
        }
        this.itemType = itemType;
        this.itemId = itemId;
        this.price = price;
    }

    void assignOrder(Order order) {
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_ITEM);
        }
        if (this.order != null && this.order != order) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEM_ALREADY_ASSIGNED);
        }
        this.order = order;
    }

    public static OrderItem createCourseItem(Long courseId, BigDecimal price) {
        return new OrderItem(ItemType.COURSE, courseId, price);
    }

    @Getter
    @RequiredArgsConstructor
    public enum ItemType {
        COURSE("강좌");

        private final String description;
    }
}
