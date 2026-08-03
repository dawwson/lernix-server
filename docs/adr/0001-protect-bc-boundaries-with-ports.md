# 0001. Port와 전용 모델로 BC 경계 보호

- 상태: Accepted
- 결정일: 2026-08-03

## 맥락

Cart, Order, Payment가 다른 BC의 Entity와 Service 구현을 직접 참조하면 제공 BC의 내부 변경이 호출자에게 전파됩니다. 같은 정보라도 Cart의 판매 상태 확인, Order의 가격 확정, Payment의 결제 가능 여부처럼 사용 목적과 필요한 데이터가 다릅니다.

## 결정

- 제공 BC는 사용 목적별 application Inbound Port와 응답 모델을 공개합니다.
- 호출 BC의 application 계층은 자기 Outbound Port에만 의존합니다.
- 호출 측 adapter가 Outbound Port를 구현하고 제공 BC의 Inbound Port를 호출합니다.
- BC 사이에서 Entity나 내부 DTO를 공유하지 않고 호출 목적에 맞는 경계 모델을 사용합니다.

## 결과

BC의 내부 모델과 호출자의 요구가 분리되고 의존 방향이 인터페이스로 드러납니다. 사용 목적이 추가되면 port와 모델이 늘어날 수 있으며, 계약 변경 시 양쪽 adapter와 테스트를 함께 관리해야 합니다.

