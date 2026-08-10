-- 기존 payments는 결제 시도 한 번을 나타낸다.
-- 기존 ID를 PaymentAttempt ID로 보존하면서 주문별 Payment aggregate로 분리한다.

rename table payments to legacy_payments;

create table payments
(
    amount       decimal(19, 0)                           not null,
    currency     varchar(3)                               not null,
    created_at   datetime(6)                              not null,
    updated_at   datetime(6)                              not null,
    user_id      bigint                                   not null,
    version      bigint       default 0                   not null,
    paid_at      datetime(6)                              null,
    canceled_at  datetime(6)                              null,
    refunded_at  datetime(6)                              null,
    id           varchar(255)                             not null primary key,
    order_id     varchar(255)                             not null,
    status       enum ('UNPAID', 'PAID', 'CANCELED', 'REFUNDED') not null,
    constraint uk_payments_order_id unique (order_id)
);

create table payment_attempts
(
    approved_at   datetime(6)                         null,
    failed_at     datetime(6)                         null,
    id            varchar(255)                        not null primary key,
    payment_id    varchar(255)                        not null,
    payment_key   varchar(255)                        null,
    payment_method enum ('CARD')                      not null,
    pg_provider   enum ('TOSS')                       not null,
    status        enum ('PENDING', 'APPROVED', 'FAILED') not null,
    constraint uk_payment_attempts_payment_key unique (payment_key),
    constraint fk_payment_attempts_payment
        foreign key (payment_id) references payments (id)
);

-- 완료된 결제를 우선하고, 그 외에는 PENDING, FAILED 순서로 주문별 대표 행을 선택한다.
create temporary table payment_root_candidates as
select id,
       row_number() over (
           partition by order_id
           order by case
                        when payment_status in ('APPROVED', 'CANCELED', 'REFUNDED') then 0
                        when payment_status = 'PENDING' then 1
                        else 2
                    end,
                    updated_at desc,
                    id desc
       ) as candidate_order
from legacy_payments;

insert into payments (
    id, user_id, order_id, amount, currency, status, version,
    paid_at, canceled_at, refunded_at, created_at, updated_at
)
select legacy.id,
       legacy.user_id,
       legacy.order_id,
       legacy.amount,
       legacy.currency,
       case legacy.payment_status
           when 'APPROVED' then 'PAID'
           when 'CANCELED' then 'CANCELED'
           when 'REFUNDED' then 'REFUNDED'
           else 'UNPAID'
       end,
       0,
       legacy.approved_at,
       legacy.canceled_at,
       legacy.refunded_at,
       aggregate_time.created_at,
       aggregate_time.updated_at
from legacy_payments legacy
join payment_root_candidates candidate
  on candidate.id = legacy.id
 and candidate.candidate_order = 1
join (
    select order_id, min(created_at) as created_at, max(updated_at) as updated_at
    from legacy_payments
    group by order_id
) aggregate_time on aggregate_time.order_id = legacy.order_id;

-- 기존 Payment 행은 모두 PaymentAttempt 이력으로 보존한다.
-- 비정상적으로 PENDING이 여러 개라면 대표 행만 PENDING으로 두고 나머지는 FAILED로 종료한다.
insert into payment_attempts (
    id, payment_id, payment_key, payment_method, pg_provider,
    status, approved_at, failed_at
)
select legacy.id,
       payment.id,
       legacy.payment_key,
       legacy.payment_method,
       legacy.pg_provider,
       case
           when legacy.payment_status in ('APPROVED', 'CANCELED', 'REFUNDED') then 'APPROVED'
           when legacy.payment_status = 'PENDING' and candidate.candidate_order = 1 then 'PENDING'
           else 'FAILED'
       end,
       case
           when legacy.payment_status in ('APPROVED', 'CANCELED', 'REFUNDED') then legacy.approved_at
           else null
       end,
       case
           when legacy.payment_status = 'FAILED'
                or (legacy.payment_status = 'PENDING' and candidate.candidate_order <> 1)
               then legacy.updated_at
           else null
       end
from legacy_payments legacy
join payment_root_candidates candidate on candidate.id = legacy.id
join payments payment on payment.order_id = legacy.order_id;

drop temporary table payment_root_candidates;
drop table legacy_payments;
