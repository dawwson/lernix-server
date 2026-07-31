package com.lxp.aplus.payment.adapter.out.order;

import com.lxp.aplus.order.application.port.in.OrderQueryToPaymentUseCase;
import com.lxp.aplus.order.application.port.in.model.result.ChargeableOrder;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderQueryAdapter implements PaymentOrderQueryPort {

    private final OrderQueryToPaymentUseCase orderQueryToPaymentUseCase;

    @Override
    public PayableOrder getPayableOrder(String orderId, Long userId) {
        ChargeableOrder order = orderQueryToPaymentUseCase.getChargeableOrder(orderId, userId);

        return PayableOrder.of(order.orderId(), order.userId(), order.amount());
    }
}
