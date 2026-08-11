create table idempotency_records
(
    user_id         bigint                              not null,
    idempotency_key varchar(255)                        not null,
    request_hash    varchar(64)                         not null,
    status          enum ('PROCESSING', 'COMPLETED')    not null,
    resource_id     varchar(255)                        null,
    created_at      datetime(6)                         not null,
    primary key (user_id, idempotency_key)
);
