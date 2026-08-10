package com.lxp.aplus.payment.application.service;

import com.lxp.aplus.payment.application.port.in.model.command.PaymentPrepareCommand;
import com.lxp.aplus.payment.application.port.out.idempotency.IdempotencyRecordStorePort;
import com.lxp.aplus.payment.domain.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PaymentIdempotencyService {

    private final IdempotencyRecordStorePort recordStore;

    /**
     * 사용자와 멱등성 키로 요청의 처리 방향을 결정한다.
     * 레코드가 없으면 최초 요청으로 등록하고, 있으면 동일 요청의 완료 결과를 재사용한다.
     */
    BeginResult begin(PaymentPrepareCommand command) {
        // 같은 키를 다른 주문에 재사용했는지 판별하기 위해 주문 ID를 고정 길이 hash로 저장한다. (fingerprint 역할)
        String requestHash = hash(command.orderId());

        return recordStore.findByUserIdAndIdempotencyKey(command.userId(), command.idempotencyKey())
                .map(record -> reuse(record, requestHash))
                .orElseGet(() -> create(command, requestHash));
    }

    void complete(IdempotencyRecord record, String resourceId) {
        // 최초 prepare 응답을 복원할 PaymentAttempt ID를 기록한다.
        record.complete(resourceId);
        recordStore.save(record);
    }

    private BeginResult reuse(IdempotencyRecord record, String requestHash) {
        // 같은 키라도 요청이 다르거나 아직 처리 중이면 기존 결과를 재사용할 수 없다.
        record.validateRequestHash(requestHash);
        return BeginResult.completed(record.completedResourceId());
    }

    private BeginResult create(PaymentPrepareCommand command, String requestHash) {
        // 결제 준비보다 먼저 PROCESSING 레코드를 저장해 동일 사용자·키의 중복 처리를 차단한다.
        IdempotencyRecord record = IdempotencyRecord.create(
                command.userId(), command.idempotencyKey(), requestHash);
        recordStore.save(record);
        return BeginResult.processing(record);
    }

    private String hash(String orderId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(orderId.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    record BeginResult(
            IdempotencyRecord record,
            String completedResourceId
    ) {

        // 최초 요청: 결제 준비 후 record를 COMPLETED로 변경해야 한다.
        static BeginResult processing(IdempotencyRecord record) {
            return new BeginResult(record, null);
        }

        // 완료된 재요청: resourceId로 최초 결과를 재현한다.
        static BeginResult completed(String resourceId) {
            return new BeginResult(null, resourceId);
        }

        boolean isCompleted() {
            return completedResourceId != null;
        }
    }
}
