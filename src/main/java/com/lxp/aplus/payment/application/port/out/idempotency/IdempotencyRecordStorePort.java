package com.lxp.aplus.payment.application.port.out.idempotency;

import com.lxp.aplus.payment.domain.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRecordStorePort {

    IdempotencyRecord save(IdempotencyRecord record);

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
