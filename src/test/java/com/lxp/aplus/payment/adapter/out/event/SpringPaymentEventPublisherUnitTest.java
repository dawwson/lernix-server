package com.lxp.aplus.payment.adapter.out.event;

import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringPaymentEventPublisherUnitTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SpringPaymentEventPublisher eventPublisher;

    @Test
    void publish_DelegatesEventToSpringPublisher() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "payment-1",
                "order-1",
                1L,
                BigDecimal.valueOf(50000)
        );

        eventPublisher.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
