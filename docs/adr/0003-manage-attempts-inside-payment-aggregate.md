# 0003. Payment Aggregate 내부에서 결제 시도 관리

- 상태: Accepted
- 결정일: 2026-08-10

## 맥락

현재 `Payment`는 PG 결제 시도 한 번을 나타냅니다. 실패 후 재시도하면 동일 주문에 여러 Payment가 생기며, 애플리케이션 서비스가 주문의 모든 Payment를 조회해 새 시도 가능 여부를 판단합니다.

이 구조에서는 첫 Payment가 생성되기 전에 Payment BC 안에서 잠글 행이 없습니다. 동일 주문의 동시 요청을 직렬화하기 위해 항상 존재하는 Order 행을 잠그는 방법을 검토했지만, Payment의 불변식을 보호하기 위해 Order BC의 persistence 구현에 의존하게 됩니다. 이는 BC 사이에서 Entity와 내부 구현을 공유하지 않는다는 [ADR 0001](0001-protect-bc-boundaries-with-ports.md)의 방향과 맞지 않습니다.

결제 준비 요청에는 다음 두 종류의 중복이 있습니다.

- 동일한 `Idempotency-Key`를 사용한 같은 요청의 재전송
- 서로 다른 키를 사용한 동일 주문의 동시 결제 시도

두 경우를 구분하면서 Payment BC가 자기 상태와 동시성 규칙을 소유할 구조가 필요합니다.

## 결정

### 주문별 Payment Aggregate

- `Payment`는 주문별 결제 프로세스를 나타내며 `order_id`에 UNIQUE 제약을 둡니다.
- 실제 PG 결제 시도 한 번은 `PaymentAttempt`로 분리하고 `Payment` Aggregate 내부에서 생성·변경합니다.
- 애플리케이션 계층은 `PaymentAttemptRepositoryPort`를 두지 않고 `PaymentRepositoryPort`로 root만 저장·조회합니다. Attempt는 Payment의 cascade로 함께 영속화합니다.
- Payment root는 첫 결제 준비 요청에서 lazy 생성합니다.
- 최초 root 생성 경쟁은 `order_id` UNIQUE 제약으로 차단합니다.
- root가 생성된 이후에는 Payment 행의 비관적 락으로 동일 주문의 시도 생성과 승인을 직렬화합니다.
- Order BC에는 주문 존재, 소유자, 결제 가능 상태와 금액만 일반 조회로 요청하며 Order 저장소의 락에는 의존하지 않습니다.

Payment의 상태 확인과 Attempt 생성은 짧은 DB 트랜잭션이며, 충돌 가능성은 낮아도 중복 결제 시도의 영향이 크므로 충돌 후 rollback하는 낙관적 락보다 요청을 먼저 직렬화하는 비관적 락을 선택합니다. 외부 PG 호출을 기다리는 동안에는 Payment 행과 DB connection을 점유하지 않도록 비관적 락을 유지하지 않습니다.

### 상태와 식별자

`Payment`는 주문별 결제의 현재 수명주기를 관리합니다.

```text
UNPAID → PAID → CANCELED
              → REFUNDED
```

`PaymentAttempt`는 개별 PG 승인 시도의 결과를 관리합니다.

```text
PENDING → APPROVED
        → FAILED
```

- 실패한 Attempt가 있어도 Payment는 `UNPAID`를 유지해 새 시도를 허용합니다.
- `PENDING` Attempt가 있으면 새 시도를 거부합니다.
- 현재 시도를 가리키는 별도 식별자는 두지 않으며, 유일한 `PENDING` Attempt를 현재 진행 중인 시도로 봅니다.
- Attempt가 승인되면 같은 트랜잭션에서 Payment를 `PAID`로 변경합니다.
- 취소와 환불은 승인 시도의 결과를 뒤집지 않으므로 Payment 상태로만 관리합니다.
- 도메인 내부 식별자는 `Payment.id`, `PaymentAttempt.id`로 둡니다.
- API는 주문별 결제를 나타내는 `paymentId`와 특정 승인 시도를 나타내는 `paymentAttemptId`를 구분합니다.

### 요청 멱등성

- 요청 멱등성은 Payment나 PaymentAttempt 컬럼에 결합하지 않고 별도 `IdempotencyRecord`로 관리합니다.
- 키 범위는 사용자 단위이며 `(user_id, idempotency_key)`를 기본 키로 사용합니다.
- 서로 다른 사용자의 요청은 독립적이므로 같은 문자열 키를 사용하더라도 충돌시키지 않습니다. 복합 키는 동일 사용자의 중복 요청만 차단하고 다른 사용자의 동일 키 사용은 허용합니다.
- 최소 필드는 `user_id`, `idempotency_key`, `request_hash`, `status`, `resource_id`, `created_at`입니다.
- `resource_id`에는 최초 응답을 재현할 `PaymentAttempt.id`를 저장합니다.
- Redis는 실제 DB 병목이나 트래픽 요구가 확인될 때 검토하며, 현재는 DB 저장소를 사용합니다.

`IdempotencyRecord.status`는 결제 승인 상태가 아니라 prepare 요청의 처리 상태를 나타냅니다.

| 상태 | 의미 |
| --- | --- |
| `PROCESSING` | 요청은 접수됐지만 반환할 결과가 아직 확정되지 않은 상태 |
| `COMPLETED` | 요청 결과가 확정되어 `resource_id`로 최초 응답을 복원할 수 있는 상태 |

따라서 `IdempotencyRecord.COMPLETED`, `PaymentAttempt.APPROVED`, `Payment.PAID`는 서로 다른 완료를 의미합니다. 멱등성 레코드가 `COMPLETED`여도 결제가 승인된 것은 아니며, prepare 요청에서 반환할 PaymentAttempt가 확정되었다는 의미입니다.

현재 변경에서는 DB UNIQUE 충돌을 종류별 `409 Conflict`로 변환하는 데까지 구현합니다. 동일 키 동시 요청에서 패배한 트랜잭션이 rollback된 뒤 기존 결과를 재조회해 성공 응답으로 복구하는 기능은 후속 변경으로 분리합니다.

## 검토한 대안

### Order 행 비관적 락

Payment가 생성되기 전에도 잠글 행이 있다는 장점이 있지만 Payment 불변식이 Order BC의 persistence 방식에 의존하므로 선택하지 않았습니다.

### Payment 전용 잠금 테이블

BC 경계는 지킬 수 있지만 비즈니스 모델에 없는 잠금 전용 행과 생명주기를 추가해야 합니다. 주문별 Payment root가 동일한 직렬화 기준점 역할을 할 수 있어 선택하지 않았습니다.

### Payment 행 낙관적 락

동일 Payment를 조회한 요청들이 각자 Attempt를 생성한 뒤 version 충돌에서 하나를 rollback하는 방식도 가능하지만, 충돌 예외 변환과 재시도 처리가 추가됩니다. 현재 임계 구역은 짧고 동일 주문의 시도 생성을 순서대로 판정하는 편이 단순하므로 비관적 락을 선택합니다. 실제 잠금 대기나 DB connection 병목이 확인되면 낙관적 락 전환을 재검토합니다.

### 조건부 UNIQUE index

`FAILED`를 제외한 Payment만 주문별 하나로 제한할 수 있지만 상태 규칙이 DB 표현식에 숨고, Payment와 결제 시도의 책임이 계속 섞여 있으므로 선택하지 않았습니다.

### Redis 분산 락

현재는 단일 애플리케이션과 단일 DB를 사용하며, TTL과 장애 복구 등 운영 복잡도를 추가할 근거가 없어 선택하지 않았습니다.

## 결과

Payment BC가 주문별 결제 상태, 시도 생성 규칙과 동시성 제어를 직접 소유합니다. Order BC와의 계약은 결제 가능 정보 조회로 제한되고, 실패한 시도 이력과 주문별 결제의 현재 상태를 별도로 설명할 수 있습니다.

반면 기존 데이터를 Payment와 PaymentAttempt로 분리하는 migration이 필요하고, prepare·confirm API와 완료 이벤트에 두 식별자를 반영해야 합니다. Payment와 현재 Attempt의 상태를 같은 트랜잭션에서 일관되게 변경해야 하며, 최초 root 및 멱등성 레코드의 UNIQUE 충돌을 비즈니스 오류나 기존 결과로 변환하는 처리가 필요합니다.
