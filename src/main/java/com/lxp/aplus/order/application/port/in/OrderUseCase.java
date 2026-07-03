package com.lxp.aplus.order.application.port.in;

import com.lxp.aplus.order.application.command.OrderCreateCommand;
import com.lxp.aplus.order.application.result.OrderCreateResult;

import java.math.BigDecimal;

public interface OrderUseCase {

    OrderCreateResult createOrder(OrderCreateCommand command);

    void completeOrder(String orderId, String approvedPaymentId, BigDecimal approvedAmount);
}
