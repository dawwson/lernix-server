package com.lxp.aplus.order.application.listener;

import com.lxp.aplus.common.event.PaymentCompletedEvent;
import com.lxp.aplus.order.application.port.in.OrderUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventListener {

    private final OrderUseCase orderUseCase;

    // Payment 저장 트랜잭션이 커밋된 이후 주문 완료를 반영합니다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("PaymentCompletedEvent 수신: {}", event);
        orderUseCase.completeOrder(event.orderId(), event.paymentId(), event.approvedAmount());
    }
}
