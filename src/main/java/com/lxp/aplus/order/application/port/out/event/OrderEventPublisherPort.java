package com.lxp.aplus.order.application.port.out.event;

import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;

public interface OrderEventPublisherPort {

    void publish(OrderCompletedEvent event);
}
