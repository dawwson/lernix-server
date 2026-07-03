package com.lxp.aplus.common.event;

import java.math.BigDecimal;

/*
 * @param paymentId      결제 ID
 * @param orderId        주문 ID
 * @param userId         결제를 완료한 사용자 ID
 * @param approvedAmount 승인된 결제 금액
 */
public record PaymentCompletedEvent(
        String paymentId,
        String orderId,
        Long userId,
        BigDecimal approvedAmount
) {
}
