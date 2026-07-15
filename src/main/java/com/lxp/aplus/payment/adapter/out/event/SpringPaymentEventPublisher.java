package com.lxp.aplus.payment.adapter.out.event;

import com.lxp.aplus.payment.application.port.out.event.PaymentEventPublisherPort;
import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPaymentEventPublisher implements PaymentEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(PaymentCompletedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
