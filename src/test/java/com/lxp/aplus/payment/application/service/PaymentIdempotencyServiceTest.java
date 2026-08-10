package com.lxp.aplus.payment.application.service;

import com.lxp.aplus.common.error.BusinessException;
import com.lxp.aplus.common.error.code.PaymentErrorCode;
import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.out.idempotency.IdempotencyRecordStorePort;
import com.lxp.aplus.payment.domain.IdempotencyRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentIdempotencyService 단위 테스트")
class PaymentIdempotencyServiceTest {

    @Mock IdempotencyRecordStorePort recordStore;
    @InjectMocks PaymentIdempotencyService service;

    @Test
    @DisplayName("처음 받은 멱등성 키는 처리 중 레코드로 저장한다")
    void begin_missingRecord_savesProcessingRecord() {
        PaymentPrepareCommand command = command("order-1");
        given(recordStore.findByUserIdAndIdempotencyKey(1L, "same-key"))
                .willReturn(Optional.empty());

        PaymentIdempotencyService.BeginResult result = service.begin(command);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.record().getStatus()).isEqualTo(IdempotencyRecord.Status.PROCESSING);
        then(recordStore).should().save(result.record());
    }

    @Test
    @DisplayName("동일한 키와 주문으로 완료된 요청을 반복하면 기존 리소스 ID를 반환한다")
    void begin_sameKeyAndOrder_returnsCompletedResourceId() {
        PaymentPrepareCommand command = command("order-1");
        IdempotencyRecord record = createRecord(command);
        service.complete(record, "attempt-1");
        given(recordStore.findByUserIdAndIdempotencyKey(1L, "same-key"))
                .willReturn(Optional.of(record));

        PaymentIdempotencyService.BeginResult result = service.begin(command);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.completedResourceId()).isEqualTo("attempt-1");
    }

    @Test
    @DisplayName("동일한 키로 다른 주문을 요청하면 충돌 예외가 발생한다")
    void begin_sameKeyAndDifferentOrder_throwsIdempotencyKeyConflict() {
        PaymentPrepareCommand originalCommand = command("order-1");
        IdempotencyRecord record = createRecord(originalCommand);
        service.complete(record, "attempt-1");
        given(recordStore.findByUserIdAndIdempotencyKey(1L, "same-key"))
                .willReturn(Optional.of(record));

        assertThatThrownBy(() -> service.begin(command("order-2")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    @DisplayName("동일한 멱등성 요청이 처리 중이면 충돌 예외가 발생한다")
    void begin_processingRecord_throwsRequestInProgress() {
        PaymentPrepareCommand command = command("order-1");
        IdempotencyRecord record = createRecord(command);
        given(recordStore.findByUserIdAndIdempotencyKey(1L, "same-key"))
                .willReturn(Optional.of(record));

        assertThatThrownBy(() -> service.begin(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_IDEMPOTENCY_REQUEST_IN_PROGRESS);
    }

    private IdempotencyRecord createRecord(PaymentPrepareCommand command) {
        given(recordStore.findByUserIdAndIdempotencyKey(command.userId(), command.idempotencyKey()))
                .willReturn(Optional.empty());
        return service.begin(command).record();
    }

    private PaymentPrepareCommand command(String orderId) {
        return new PaymentPrepareCommand(1L, orderId, "same-key");
    }
}
