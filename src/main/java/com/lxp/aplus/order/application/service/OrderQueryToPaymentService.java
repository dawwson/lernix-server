package com.lxp.aplus.order.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.order.application.port.in.OrderQueryToPaymentUseCase;
import com.lxp.aplus.order.application.port.in.model.result.ChargeableOrder;
import com.lxp.aplus.order.application.port.out.repository.OrderRepositoryPort;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryToPaymentService implements OrderQueryToPaymentUseCase {

    private final OrderRepositoryPort orderRepository;

    @Override
    public ChargeableOrder getChargeableOrder(String orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_STATUS);
        }

        return ChargeableOrder.from(order);
    }
}
