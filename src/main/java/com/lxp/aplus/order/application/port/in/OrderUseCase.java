package com.lxp.aplus.order.application.port.in;

import com.lxp.aplus.order.application.command.OrderCreateCommand;
import com.lxp.aplus.order.application.result.OrderCreateResult;

public interface OrderUseCase {

    OrderCreateResult createOrder(OrderCreateCommand command);
}
