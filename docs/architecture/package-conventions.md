# 패키지 컨벤션

## 적용 범위

이 컨벤션은 Cart, Order, Payment, Course BC에 적용합니다. Cart, Order, Payment는 아래 구조를 사용하고 있으며, Course는 구매 관련 Inbound Port부터 점진적으로 적용하는 전환 상태입니다.

기능 변경과 무관한 패키지를 컨벤션 적용만을 위해 함께 이동하지 않습니다.

## 기본 구조

```text
<bc>
├── adapter
│   ├── in
│   └── out
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   └── service
└── domain
```

| 패키지 | 책임 |
| --- | --- |
| `adapter.in` | HTTP 요청이나 이벤트처럼 외부 입력을 application 호출로 변환 |
| `adapter.out` | DB, 다른 BC, 이벤트 발행 등 외부 의존성과 연결 |
| `application.port.in` | BC가 제공하는 Use Case와 입력·결과 계약 |
| `application.port.out` | application이 필요로 하는 저장소·다른 BC·이벤트 발행 계약 |
| `application.service` | Inbound Port 구현과 Use Case 조율 |
| `domain` | Entity, Value Object, 상태 전이와 비즈니스 규칙 |

## 경계 모델 소유권

- Command와 Result 등 Inbound 모델은 이를 공개하는 BC의 `application.port.in` 아래에 둡니다.
- Outbound 모델은 이를 필요로 하는 호출 BC의 해당 `application.port.out` 아래에 둡니다.
- 다른 BC의 Entity나 내부 DTO를 경계 모델로 재사용하지 않습니다.
- Web Request와 Response는 `adapter.in.web`에 두고 application 경계 모델과 분리합니다.

다른 BC를 호출하는 구체적인 연결 방식은 [Bounded Context 경계](bounded-context-boundaries.md)를 참고합니다.

## 이벤트 위치

- 발행 이벤트 모델은 발행 BC의 `application.port.out.event.model`에 둡니다.
- Event Publisher 인터페이스는 발행 BC의 `application.port.out.event`에 둡니다.
- Spring 기반 발행 구현은 발행 BC의 `adapter.out.event`에 둡니다.
- 이벤트 listener는 소비 BC의 `adapter.in.event`에 둡니다.

예를 들어 `PaymentCompletedEvent`는 Payment가 소유하고 Order의 Inbound Event Adapter가 소비합니다. `OrderCompletedEvent`는 Order가 소유하고 Enrollment가 소비합니다.

## Course 전환 기준

Course의 기존 `presentation`, `infrastructure`, application 내부 패키지는 현재 구조로 인정합니다. Cart와 Order에 제공하는 구매 조회 계약은 `application.port.in.cart`, `application.port.in.order`에 두며, 이후 패키지 전환은 해당 영역을 변경하는 PR에서 필요한 범위만 수행합니다.
