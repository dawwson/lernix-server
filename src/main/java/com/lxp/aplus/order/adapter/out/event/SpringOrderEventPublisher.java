package com.lxp.aplus.order.adapter.out.event;

import com.lxp.aplus.order.application.port.out.event.OrderEventPublisherPort;
import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringOrderEventPublisher implements OrderEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(OrderCompletedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
