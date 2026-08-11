# 0004. DB UNIQUE 제약으로 paymentKey 유일성 보장

- 상태: Accepted
- 결정일: 2026-08-11

## 맥락

`paymentKey`는 PG가 발급한 승인 식별자이며 모든 `PaymentAttempt`에서 유일해야 합니다. 동일한 `paymentKey`를 같은 승인 요청에 다시 사용하는 것은 멱등한 재요청으로 처리하지만, 다른 Payment나 PaymentAttempt에 사용하는 것은 정합성 오류입니다.

승인 전에 `paymentKey`를 조회해 사용 여부를 검사하는 방법을 검토했습니다. 그러나 조회와 저장은 하나의 원자적 연산이 아닙니다. 서로 다른 Payment를 승인하는 두 트랜잭션은 서로 다른 Payment 행을 잠그므로 다음 순서로 실행될 수 있습니다.

```text
트랜잭션 A: paymentKey 조회 → 없음
트랜잭션 B: paymentKey 조회 → 없음
트랜잭션 A: paymentKey 저장 → 성공
트랜잭션 B: paymentKey 저장 → 중복 생성 가능
```

이 race condition은 격리 수준과 별도의 범위 잠금에 의존하지 않는 한 애플리케이션의 사전 조회만으로 차단할 수 없습니다. Payment별 비관적 락도 동일 Payment의 동시 승인만 직렬화하며, 서로 다른 Payment 사이의 전역 `paymentKey` 유일성까지 보호하지 않습니다.

## 결정

- `payment_attempts.payment_key`의 `uk_payment_attempts_payment_key` UNIQUE 제약을 유일성의 최종 기준으로 사용합니다.
- 승인 전에 별도 존재 여부 조회를 추가하지 않습니다.
- 승인 저장에는 `saveAndFlush`를 사용해 현재 트랜잭션 안에서 UNIQUE 위반을 확인합니다.
- 해당 제약 위반은 `PAYMENT_APPROVAL_CONFLICT`로 변환해 `409 Conflict`로 응답합니다.
- 동일 Payment에 대한 재요청인지는 Payment 행을 잠근 뒤 도메인이 `paymentAttemptId`, `paymentKey`, `amount`를 비교해 판정합니다. UNIQUE 제약은 서로 다른 Payment나 동시 트랜잭션 사이의 충돌을 담당합니다.

사전 조회는 충돌 가능성을 미리 알려 줄 수는 있지만 정확성을 보장하지 못하며, 정상 승인마다 조회 쿼리를 하나 추가합니다. 현재는 DB 제약만으로 정확성과 오류 변환을 모두 충족하므로 사전 조회를 두지 않습니다.

## 결과

서로 다른 Payment가 동시에 같은 `paymentKey`를 저장하더라도 DB가 하나만 허용하므로 전역 유일성이 보장됩니다. 애플리케이션은 특정 DB 제약 이름을 비즈니스 오류에 매핑해야 하며, 충돌한 트랜잭션은 rollback됩니다.

향후 사전 조회를 추가하더라도 사용자에게 더 빠른 오류를 제공하는 최적화로만 사용하며 UNIQUE 제약은 제거하지 않습니다. DB를 분리해 단일 UNIQUE 제약으로 전역 유일성을 보장할 수 없게 되면 별도의 전역 식별자 저장소나 조정 방식을 다시 결정해야 합니다.
