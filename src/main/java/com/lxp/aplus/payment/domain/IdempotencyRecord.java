package com.lxp.aplus.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @EmbeddedId
    private Id id;

    @Column(nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column
    private String resourceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private IdempotencyRecord(Id id, String requestHash) {
        this.id = id;
        this.requestHash = requestHash;
        this.status = Status.PROCESSING;
        this.createdAt = LocalDateTime.now();
    }

    public static IdempotencyRecord create(Long userId, String idempotencyKey, String requestHash) {
        return new IdempotencyRecord(new Id(userId, idempotencyKey), requestHash);
    }

    public void complete(String resourceId) {
        this.status = Status.COMPLETED;
        this.resourceId = resourceId;
    }

    public Long getUserId() {
        return id.userId;
    }

    public String getIdempotencyKey() {
        return id.idempotencyKey;
    }

    public enum Status { PROCESSING, COMPLETED }

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Id implements Serializable {

        @Column(name = "user_id", nullable = false, updatable = false)
        private Long userId;

        @Column(name = "idempotency_key", nullable = false, updatable = false)
        private String idempotencyKey;

        private Id(Long userId, String idempotencyKey) {
            this.userId = userId;
            this.idempotencyKey = idempotencyKey;
        }

        public static Id of(Long userId, String idempotencyKey) {
            return new Id(userId, idempotencyKey);
        }
    }
}
