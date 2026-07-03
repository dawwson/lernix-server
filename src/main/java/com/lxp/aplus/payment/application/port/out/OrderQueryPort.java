package com.lxp.aplus.payment.application.port.out;

import com.lxp.aplus.payment.application.result.PayableOrder;

public interface OrderQueryPort {

    PayableOrder getPayableOrder(String orderId, Long userId);
}
