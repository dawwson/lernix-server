package com.lxp.aplus.payment.adapter.out.order;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.OrderErrorCode;
import com.lxp.aplus.order.application.port.in.OrderQueryToPaymentUseCase;
import com.lxp.aplus.order.application.port.in.model.result.ChargeableOrder;
import com.lxp.aplus.payment.application.port.out.order.model.PayableOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrderQueryAdapter 단위 테스트")
class PaymentOrderQueryAdapterUnitTest {

    @Mock
    private OrderQueryToPaymentUseCase orderQueryToPaymentUseCase;

    @InjectMocks
    private PaymentOrderQueryAdapter adapter;

    @Test
    @DisplayName("Order의 결제 가능 주문 정보를 Payment 내부 모델로 변환한다")
    void getPayableOrder_Success() {
        String orderId = "order-1";
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50000);
        given(orderQueryToPaymentUseCase.getChargeableOrder(orderId, userId))
                .willReturn(new ChargeableOrder(orderId, userId, amount));

        PayableOrder result = adapter.getPayableOrder(orderId, userId);

        then(orderQueryToPaymentUseCase).should().getChargeableOrder(orderId, userId);
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.amount()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("Order 조회 예외를 변경하지 않고 전달한다")
    void getPayableOrder_PropagatesException() {
        String orderId = "order-1";
        Long userId = 1L;
        BusinessException exception = new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        given(orderQueryToPaymentUseCase.getChargeableOrder(orderId, userId))
                .willThrow(exception);

        assertThatThrownBy(() -> adapter.getPayableOrder(orderId, userId))
                .isSameAs(exception);
    }
}
