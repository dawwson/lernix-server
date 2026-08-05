# API 응답 포맷

이 문서는 HTTP API의 성공·실패 응답 구조와 응답 코드를 추가할 때 지켜야 할 규칙을 설명합니다.

## 성공 응답

일반적인 성공 응답은 `ResultResponse<T>`로 감쌉니다.

```json
{
  "status": "CREATED",
  "code": "SC001",
  "message": "강좌가 정상적으로 등록되었습니다.",
  "data": {
    "courseId": 1
  }
}
```

- `status`: 실제 HTTP 상태와 같은 `HttpStatus` 이름
- `code`: 클라이언트가 성공 결과를 식별하는 코드
- `message`: 사용자에게 전달할 수 있는 성공 메시지
- `data`: API별 응답 데이터

데이터가 있으면 `ResultResponse.of(resultCode, data)`, 없으면 `ResultResponse.from(resultCode)`를 사용합니다. 데이터가 없는 성공 응답에도 `data: null`이 포함됩니다.

컨트롤러의 HTTP 상태는 `ResultCode.status`와 같아야 합니다.

## 실패 응답

비즈니스 규칙 위반은 `BusinessException`에 `ErrorCode`를 담아 발생시킵니다. `GlobalExceptionHandler`는 이를 `ErrorResponse`로 변환합니다.

```json
{
  "status": "NOT_FOUND",
  "code": "EC001",
  "message": "해당 강좌를 찾을 수 없습니다."
}
```

- `status`: 실제 HTTP 상태와 같은 `HttpStatus` 이름
- `code`: 클라이언트가 오류 원인을 식별하는 코드
- `message`: 내부 구현이나 비밀값을 노출하지 않는 오류 설명

요청 본문과 파라미터 검증 오류도 같은 실패 응답 구조를 사용합니다. 검증 오류의 `message`에는 첫 번째 실패 필드와 검증 메시지가 포함될 수 있습니다.

Spring Security의 인증·인가 실패도 `ErrorResponse` 구조로 반환합니다.

## 응답 코드

성공 코드는 `S`, 오류 코드는 `E`로 시작합니다. 그 뒤에 도메인을 나타내는 1~2글자 약어와 세 자리 일련번호를 사용합니다.

```text
성공: S + 도메인 약어 + 세 자리 번호
오류: E + 도메인 약어 + 세 자리 번호
```

예시는 다음과 같습니다.

```text
SC001  Course 성공
SPM001 Payment 성공
EC001  Course 오류
EPM001 Payment 오류
EG001  공통 오류
```

현재 코드에서 사용하는 도메인 약어는 다음과 같습니다.

| 약어 | 영역 | 비고 |
| --- | --- | --- |
| `G` | Global | 인증, 권한, 입력값 검증과 공통 오류 |
| `U` | User | 사용자와 인증 관련 결과 |
| `CT` | Category | 강좌 카테고리 |
| `C` | Course | 강좌 |
| `S` | Section | 강좌 섹션 |
| `L` | Lecture | 강의 |
| `E` | Enrollment | 수강 |
| `P` | Progress | 학습 진행률 |
| `PM` | Payment | 결제 |
| `O` | Order | 주문 |
| `R` | Review | 리뷰 |

Cart는 현재 `O`, Lecture Resource는 현재 `R`을 사용하여 각각 Order, Review와 충돌합니다. 이는 새 코드에 적용할 약어 규칙이 아니라 기존 호환성 제약입니다.

새 코드를 추가할 때는 다음을 지킵니다.

- 도메인별 `ResultCode` 또는 `ErrorCode` 구현 enum에 추가합니다.
- 기존 코드의 의미를 다른 용도로 바꾸지 않습니다.
- HTTP 상태, 코드, 메시지를 함께 정의합니다.
- 저장소 전체에서 같은 코드가 이미 사용되는지 확인합니다.
- 공개된 코드는 클라이언트와의 계약이므로 단순 정리를 목적으로 변경하지 않습니다.

응답 코드의 최신 목록과 정확한 HTTP 상태·메시지는 `common.result.code`와 `common.error.code`의 enum을 기준으로 확인합니다. 문서에 enum 상수 전체를 복제하지 않습니다.

## HTTP 상태

응답 본문의 `status`는 실제 HTTP 상태와 동일해야 합니다. 현재 응답 코드에서 주로 사용하는 상태는 다음과 같습니다.

| 상태 | 코드 | 사용 기준 |
| --- | --- | --- |
| `OK` | 200 | 조회·수정·삭제와 동기 처리 완료 |
| `CREATED` | 201 | 리소스 생성 완료 |
| `ACCEPTED` | 202 | 요청을 접수했으나 후속 처리가 남은 경우 |
| `BAD_REQUEST` | 400 | 요청 형식이나 값이 유효하지 않은 경우 |
| `UNAUTHORIZED` | 401 | 인증이 필요하거나 토큰이 만료된 경우 |
| `FORBIDDEN` | 403 | 인증됐지만 권한이 없는 경우 |
| `NOT_FOUND` | 404 | 대상 리소스를 찾을 수 없는 경우 |
| `CONFLICT` | 409 | 현재 상태나 기존 데이터와 충돌하는 경우 |
| `INTERNAL_SERVER_ERROR` | 500 | 예상하지 못한 서버 오류 |

## 현재 예외와 제약

대부분의 API가 공통 응답 포맷을 사용하지만 다음 예외가 있습니다.

- 강좌 썸네일 업로드 URL API는 성공 결과를 `ResultResponse`로 감싸지 않습니다.
- 사용자 정보가 없을 때 일부 조회 API는 `ErrorResponse` 없이 빈 `404` 응답을 반환합니다.
- Cart와 Order는 `SO001`, `EO001`~`EO003`을 중복 사용합니다.
- Review와 Lecture Resource는 `ER001`~`ER007`을 중복 사용합니다.

이 항목은 현재 구현의 예외입니다. 새 API에서 같은 방식을 확장하지 않으며, 기존 코드는 API 호환성을 검토하는 별도 변경에서 정리합니다.

## 새 API 완료 조건

- 성공 응답은 `ResultResponse<T>`를 사용합니다.
- 실패 응답은 `ErrorResponse`를 사용합니다.
- 응답 본문의 `status`와 실제 HTTP 상태가 일치합니다.
- 응답 코드가 저장소 전체에서 중복되지 않습니다.
- Controller 테스트에서 HTTP 상태, `code`, 주요 `data` 또는 오류 `message`를 검증합니다.
