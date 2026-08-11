package com.lxp.aplus.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotencyRecord 도메인 테스트")
class IdempotencyRecordTest {

    @Test
    @DisplayName("멱등성 레코드는 처리 중 상태로 생성된다")
    void create_validValues_returnsProcessingRecord() {
        IdempotencyRecord record = IdempotencyRecord.create(1L, "idempotency-key", "request-hash");

        assertThat(record.getUserId()).isEqualTo(1L);
        assertThat(record.getIdempotencyKey()).isEqualTo("idempotency-key");
        assertThat(record.getRequestHash()).isEqualTo("request-hash");
        assertThat(record.getStatus()).isEqualTo(IdempotencyRecord.Status.PROCESSING);
        assertThat(record.getResourceId()).isNull();
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("처리를 완료하면 생성된 결제 시도 ID를 기록한다")
    void complete_processingRecord_recordsResourceId() {
        IdempotencyRecord record = IdempotencyRecord.create(1L, "idempotency-key", "request-hash");

        record.complete("payment-attempt-1");

        assertThat(record.getStatus()).isEqualTo(IdempotencyRecord.Status.COMPLETED);
        assertThat(record.getResourceId()).isEqualTo("payment-attempt-1");
    }
}
