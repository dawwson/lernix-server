# 0002. 구매 완료 후속 처리를 프로세스 내부 이벤트로 분리

- 상태: Accepted
- 결정일: 2026-07-15

## 맥락

결제 승인 과정에서 Payment가 주문 완료와 수강권 생성까지 직접 조율하면 Payment가 Order와 Enrollment의 상태 변경 규칙을 알게 됩니다. 각 BC가 자기 상태 전이를 책임지면서도 현재의 단일 애플리케이션 안에서 구매 완료 흐름을 이어갈 방법이 필요했습니다.

메시지 브로커와 Outbox를 도입하는 방법도 있지만, 현재 시스템은 하나의 프로세스로 배포되며 운영 복잡도를 늘리지 않는 것이 우선입니다.

## 결정

- Payment는 결제 승인 후 `PaymentCompletedEvent`를 발행합니다.
- Order는 이벤트를 받아 주문을 완료하고 `OrderCompletedEvent`를 발행합니다.
- Enrollment는 주문 완료 이벤트를 받아 수강권을 생성합니다.
- application service는 Event Publisher Outbound Port에 의존하고 Spring Application Event 구현은 Outbound Adapter에 둡니다.
- 이벤트 listener는 트랜잭션 커밋 이후 실행하며, 현재는 메시지 브로커나 Outbox를 사용하지 않습니다.

## 결과

Payment가 다른 BC의 상태 변경을 직접 호출하지 않고 각 BC가 자기 규칙을 소유합니다. 반면 후속 처리는 원 트랜잭션과 원자적이지 않고 프로세스 장애 시 유실될 수 있습니다. Enrollment의 일부 데이터 접근 장애는 재시도로 보완하지만, 강한 전달 보장이 필요해지면 Outbox와 메시지 브로커 도입을 별도 ADR로 결정해야 합니다.

