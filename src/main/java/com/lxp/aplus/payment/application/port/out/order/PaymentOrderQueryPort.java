package com.lxp.aplus.payment.application.port.out.order;

import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;

public interface PaymentOrderQueryPort {

    PayableOrder getPayableOrder(String orderId, Long userId);
}
