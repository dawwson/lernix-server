package com.lxp.aplus.payment.application.port.out.event.model;

import java.math.BigDecimal;

/**
 * Payment BC가 발행하는 결제 승인 완료 이벤트입니다.
 */
public record PaymentCompletedEvent(
        String paymentId,
        String orderId,
        Long userId,
        BigDecimal approvedAmount
) {
}
