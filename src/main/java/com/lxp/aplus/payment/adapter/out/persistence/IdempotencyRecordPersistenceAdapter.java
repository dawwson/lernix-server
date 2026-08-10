package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.application.port.out.idempotency.IdempotencyRecordStorePort;
import com.lxp.aplus.payment.domain.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyRecordPersistenceAdapter implements IdempotencyRecordStorePort {

    private final IdempotencyRecordJpaRepository jpaRepository;

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return jpaRepository.saveAndFlush(record);
    }

    @Override
    public Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey) {
        return jpaRepository.findById(IdempotencyRecord.Id.of(userId, idempotencyKey));
    }
}
