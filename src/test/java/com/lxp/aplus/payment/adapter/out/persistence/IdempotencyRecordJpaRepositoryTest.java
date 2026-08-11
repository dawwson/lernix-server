package com.lxp.aplus.payment.adapter.out.persistence;

import com.lxp.aplus.payment.domain.IdempotencyRecord;
import com.lxp.aplus.testing.config.PersistenceTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PersistenceTestConfiguration.class)
@DisplayName("IdempotencyRecordJpaRepository 테스트")
class IdempotencyRecordJpaRepositoryTest {

    @Autowired
    private IdempotencyRecordJpaRepository repository;

    @Test
    @DisplayName("같은 사용자의 멱등성 키로 저장된 레코드를 조회한다")
    void findById_existingRecord_returnsRecord() {
        IdempotencyRecord record = IdempotencyRecord.create(1L, "same-key", "request-hash");
        repository.saveAndFlush(record);

        IdempotencyRecord savedRecord = repository
                .findById(IdempotencyRecord.Id.of(1L, "same-key"))
                .orElseThrow();

        assertThat(savedRecord.getUserId()).isEqualTo(1L);
        assertThat(savedRecord.getIdempotencyKey()).isEqualTo("same-key");
        assertThat(savedRecord.getRequestHash()).isEqualTo("request-hash");
    }

    @Test
    @DisplayName("다른 사용자는 동일한 멱등성 키를 각각 사용할 수 있다")
    void save_sameKeyForDifferentUsers_savesBothRecords() {
        repository.save(IdempotencyRecord.create(1L, "same-key", "request-hash-1"));
        repository.save(IdempotencyRecord.create(2L, "same-key", "request-hash-2"));
        repository.flush();

        assertThat(repository.count()).isEqualTo(2);
    }
}
