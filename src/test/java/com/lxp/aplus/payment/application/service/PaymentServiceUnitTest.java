package com.lxp.aplus.payment.application.service;

import com.lxp.aplus.payment.application.port.in.model.command.PaymentConfirmCommand;
import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import com.lxp.aplus.payment.application.port.out.order.PaymentOrderQueryPort;
import com.lxp.aplus.payment.application.port.out.repository.PaymentRepositoryPort;
import com.lxp.aplus.payment.domain.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceUnitTest {

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private PaymentOrderQueryPort orderQueryPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 승인 성공 시 PaymentCompletedEvent를 발행해야 한다")
    void confirm_PublishPaymentCompletedEvent_Success() {
        // given
        String orderId = "order-1";
        Long userId = 1L;
        String paymentKey = "payment-key";
        BigDecimal amount = BigDecimal.valueOf(50000);
        Payment payment = Payment.create(orderId, userId, amount);

        given(paymentRepository.findByOrderId(orderId)).willReturn(Optional.of(payment));

        // when
        paymentService.confirm(new PaymentConfirmCommand(userId, orderId, paymentKey, amount));

        // then
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.paymentId()).isEqualTo(payment.getPaymentId());
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.approvedAmount()).isEqualByComparingTo(amount);
    }
}
