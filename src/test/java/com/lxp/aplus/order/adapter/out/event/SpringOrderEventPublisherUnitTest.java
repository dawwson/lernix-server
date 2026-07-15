package com.lxp.aplus.order.adapter.out.event;

import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringOrderEventPublisherUnitTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SpringOrderEventPublisher eventPublisher;

    @Test
    void publish_DelegatesEventToSpringPublisher() {
        OrderCompletedEvent event = new OrderCompletedEvent(
                "order-1",
                1L,
                List.of(new OrderCompletedEvent.Item(10L, 100L))
        );

        eventPublisher.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
