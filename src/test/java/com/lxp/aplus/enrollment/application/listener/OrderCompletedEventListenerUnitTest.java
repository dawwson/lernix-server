package com.lxp.aplus.enrollment.application.listener;

import com.lxp.aplus.enrollment.application.command.EnrollmentCommand;
import com.lxp.aplus.enrollment.application.port.in.EnrollmentCommandUseCase;
import com.lxp.aplus.order.application.port.out.event.model.OrderCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCompletedEventListener 단위 테스트")
class OrderCompletedEventListenerUnitTest {

    @Mock
    private EnrollmentCommandUseCase enrollmentCommandUseCase;

    @InjectMocks
    private OrderCompletedEventListener listener;

    @Test
    @DisplayName("주문 완료 이벤트 수신 시 주문 항목 수만큼 수강 신청을 요청해야 한다")
    void handleOrderCompletedEvent_EnrollOrderItems_Success() {
        // given
        Long userId = 1L;
        OrderCompletedEvent event = new OrderCompletedEvent(
                "order-1",
                userId,
                List.of(
                        new OrderCompletedEvent.Item(10L, 100L),
                        new OrderCompletedEvent.Item(20L, 200L)
                )
        );

        // when
        listener.handleOrderCompletedEvent(event);

        // then
        ArgumentCaptor<EnrollmentCommand> commandCaptor = ArgumentCaptor.forClass(EnrollmentCommand.class);
        verify(enrollmentCommandUseCase, times(2)).enroll(commandCaptor.capture());

        assertThat(commandCaptor.getAllValues())
                .extracting(EnrollmentCommand::studentId, EnrollmentCommand::courseId, EnrollmentCommand::orderItemId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(userId, 10L, 100L),
                        org.assertj.core.groups.Tuple.tuple(userId, 20L, 200L)
                );
    }
}
