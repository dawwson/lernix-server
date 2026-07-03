package com.lxp.aplus.order.application.listener;

import com.lxp.aplus.common.event.PaymentCompletedEvent;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCompletedEventListener 단위 테스트")
class PaymentCompletedEventListenerUnitTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private PaymentCompletedEventListener listener;

    @Test
    @DisplayName("결제 완료 이벤트 수신 시 주문 완료 처리를 요청해야 한다")
    void handlePaymentCompletedEvent_CompleteOrder_Success() {
        // given
        String paymentId = "payment-1";
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal approvedAmount = BigDecimal.valueOf(40000);
        PaymentCompletedEvent event = new PaymentCompletedEvent(paymentId, orderId, userId, approvedAmount);

        // when
        listener.handlePaymentCompletedEvent(event);

        // then
        verify(orderUseCase).completeOrder(orderId, paymentId, approvedAmount);
    }
}
