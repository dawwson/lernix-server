package com.lxp.aplus.payment.application.port.out.event;

import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;

public interface PaymentEventPublisherPort {

    void publish(PaymentCompletedEvent event);
}
