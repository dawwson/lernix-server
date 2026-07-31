package com.lxp.aplus.order.application.port.in;

import com.lxp.aplus.order.application.port.in.model.result.ChargeableOrder;

public interface OrderQueryToPaymentUseCase {

    ChargeableOrder getChargeableOrder(String orderId, Long userId);
}
