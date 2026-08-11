# Architecture Decision Records

ADR은 대안이 있었고 장기적으로 영향을 주는 기술 결정과 그 이유를 보존합니다.

## 작성 기준

- 프레임워크나 저장 방식 선택
- 모듈 경계 또는 의존 방향 변경
- 데이터 일관성이나 이벤트 전달 방식 결정
- 보안, 호환성, 운영에 장기 영향을 주는 결정

단순 구현 세부사항, 코드 스타일, PR 요약은 ADR로 작성하지 않습니다.

## 규칙

- [`../templates/adr.md`](../templates/adr.md)를 복사해 작성합니다.
- 파일명은 `0001-short-title.md`처럼 연속 번호와 짧은 kebab-case 제목을 사용합니다.
- 상태는 다음 중 하나를 사용합니다.
  - `Proposed`: 검토 중인 제안이며 아직 최종 결정되지 않았습니다.
  - `Accepted`: 검토를 마치고 적용하기로 결정했습니다.
  - `Superseded`: 이후 ADR의 결정으로 대체되어 더 이상 현재 기준이 아닙니다.
- 승인된 ADR의 결정을 바꿀 때 원문을 고치지 않고 새 ADR에서 대체 관계를 연결합니다.
- 새 ADR을 추가하면 아래 인덱스에도 링크합니다.

## 목록

- [0001. Port와 전용 모델로 BC 경계 보호](0001-protect-bc-boundaries-with-ports.md) — Accepted
- [0002. 구매 완료 후속 처리를 프로세스 내부 이벤트로 분리](0002-use-in-process-events-for-purchase-completion.md) — Accepted
- [0003. Payment Aggregate 내부에서 결제 시도 관리](0003-manage-attempts-inside-payment-aggregate.md) — Accepted
- [0004. DB UNIQUE 제약으로 paymentKey 유일성 보장](0004-enforce-payment-key-uniqueness-in-database.md) — Accepted
