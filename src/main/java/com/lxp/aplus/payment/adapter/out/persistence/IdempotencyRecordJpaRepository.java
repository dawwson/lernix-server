package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordJpaRepository
        extends JpaRepository<IdempotencyRecord, IdempotencyRecord.Id> {
}
