# Repository Guidelines

## Project

- Java 17과 Spring Boot 3.4 기반의 온라인 학습 플랫폼 백엔드다.
- 애플리케이션 코드는 `src/main/java/com/lxp/aplus`, 테스트는 `src/test/java/com/lxp/aplus`에 있다.
- 데이터베이스 마이그레이션은 `src/main/resources/db/migration`, API 호출 예시는 `http`, 프로젝트 문서는 `docs`에 있다.
- 주요 명령은 `./gradlew test`, `./gradlew build`, `./gradlew bootRun`이다.

## Working Agreements

- 요청 범위와 무관한 사용자 변경을 되돌리거나 덮어쓰지 않는다.
- 구현을 변경했다면 관련 테스트를 실행한다. 전체 테스트를 실행하지 못하면 실행한 범위와 이유를 알린다.
- 코드에 존재하지 않는 동작이나 계획을 현재 사실처럼 기록하지 않는다.
- 비밀값, 운영 데이터, 개인 로컬 경로를 문서와 예제에 넣지 않는다.

## Documentation

- 문서는 한국어를 기본으로 작성하고 코드 식별자, API, 패턴명 등 기술 용어는 원문을 유지한다.
- 문서 진입점과 분류 기준은 `docs/README.md`를 따른다.
- 시스템 구조나 주요 흐름이 바뀌면 `docs/architecture`를 갱신한다.
- 대안이 있었고 장기 영향이 있는 기술 결정을 내리면 `docs/adr`에 ADR을 추가한다.
- 재발 가능하고 원인 파악이 어려운 문제를 해결하면 `docs/troubleshooting`에 기록한다.
- 실행 방법이나 요구 환경이 바뀌면 루트 `README.md`를 갱신한다.
- 변경과 직접 관련된 문서만 수정한다. 문서화할 내용이 없으면 문서를 억지로 추가하지 않는다.
- 문서 하나는 질문 하나에 답하도록 유지하고, 코드에서 쉽게 알 수 있는 클래스·메서드 목록은 복제하지 않는다.
- Architecture의 설명과 Mermaid 다이어그램은 현재 구현과 대조한다.

