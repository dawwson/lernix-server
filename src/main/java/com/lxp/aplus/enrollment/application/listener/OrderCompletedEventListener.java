package com.lxp.aplus.enrollment.application.listener;

import com.lxp.aplus.common.event.OrderCompletedEvent;
import com.lxp.aplus.enrollment.application.command.EnrollmentCommand;
import com.lxp.aplus.enrollment.application.port.in.EnrollmentCommandUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 완료 이벤트를 수신하여 수강 신청을 처리하는 리스너입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final EnrollmentCommandUseCase enrollmentCommandUseCase;

    /**
     * OrderCompletedEvent를 비동기 및 재시도 가능하게 처리하여 수강 신청 로직을 실행.
     * - @Async: 이벤트 발행자(Order)의 트랜잭션과 분리하여 별도의 스레드에서 비동기로 실행.
     * - @Retryable: DB 접근 등 일시적인 오류(DataAccessException) 발생 시, 1초 간격으로 최대 3번까지 재시도.
     *
     * @param event 주문 완료 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    @Retryable(
            retryFor = DataAccessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("OrderCompletedEvent 수신 (시도): {}", event);
        for (OrderCompletedEvent.Item item : event.items()) {
            EnrollmentCommand command = new EnrollmentCommand(
                    event.userId(),
                    item.courseId(),
                    item.orderItemId()
            );
            enrollmentCommandUseCase.enroll(command);
            log.info("수강 신청 처리 완료 for userId: {}, courseId: {}", event.userId(), item.courseId());
        }
    }

    /**
     * handleOrderCompletedEvent 메서드가 재시도를 모두 실패했을 때 호출되는 복구 메서드.
     * - @Recover: @Retryable에서 지정된 예외가 최종적으로 실패했을 때 실행.
     *
     * @param e     재시도를 모두 실패하게 만든 최종 예외
     * @param event 실패한 원본 이벤트
     */
    @Recover
    public void recover(DataAccessException e, OrderCompletedEvent event) {
        log.error("[CRITICAL] 수강 신청 이벤트 최종 실패. 수동 처리 필요. event: {}, error: {}", event, e.getMessage());
        // TODO: 운영자에게 알림을 보내는 로직 추가 (예: 슬랙, 이메일, SMS)
    }
}
