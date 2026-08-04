package com.lxp.aplus.order.adapter.in.event;

import com.lxp.aplus.order.application.port.in.OrderUseCase;
import com.lxp.aplus.payment.application.port.out.event.model.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PaymentCompletedEvent를 수신하여 Order UseCase를 호출하는 Inbound Event Adapter.
 * - 이벤트별 책임을 분리하여 단일 책임 원칙(SRP)을 유지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventListener {

    private final OrderUseCase orderUseCase;

    // Payment 저장 트랜잭션이 커밋된 이후 주문 완료를 반영합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("PaymentCompletedEvent 수신: {}", event);
        orderUseCase.completeOrder(event.orderId(), event.paymentId(), event.approvedAmount());
    }
}
