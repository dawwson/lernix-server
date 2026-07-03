package com.lxp.aplus.payment.infrastructure.adapter;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.payment.application.result.OrderItemSnapshot;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderRepository;
import com.lxp.aplus.payment.application.port.out.OrderCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class OrderCommandAdapter implements OrderCommandPort {

    private final OrderRepository orderRepository;

    public void completeOrder(String orderId, String approvedPaymentId, BigDecimal approvedAmount) {

        // 1. Order 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        // 2. Order 상태 변경
        order.completeWithApprovedPayment(approvedPaymentId, approvedAmount);
    }

    public List<OrderItemSnapshot> getOrderItemsOfOrder(String orderId) {

        // 1. Order 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        // 2. Order의 OrderItem 리스트 조회
        return order.getOrderItems().stream()
                .map(orderItem -> OrderItemSnapshot.of(
                        orderItem.getItemId(),
                        orderItem.getOrderItemId()
                ))
                .toList();
    }

}
