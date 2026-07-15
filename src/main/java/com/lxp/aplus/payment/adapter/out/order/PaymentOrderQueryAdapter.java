package com.lxp.aplus.payment.adapter.out.order;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.order.application.port.out.repository.OrderRepositoryPort;
import com.lxp.aplus.order.domain.Order;
import com.lxp.aplus.order.domain.OrderStatus;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentOrderQueryAdapter implements PaymentOrderQueryPort {

    private final OrderRepositoryPort orderRepository;

    @Override
    public PayableOrder getPayableOrder(String orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        // FIXME: PENDING 상태 검증은 Order BC의 정책이므로 Order application port에서 검증된 결과만 넘기도록 옮긴다.
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID_STATUS);
        }

        return PayableOrder.of(order.getOrderId(), order.getUserId(), order.getAmount());
    }
}
