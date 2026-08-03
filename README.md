# Lernix Server

온라인 강의의 회원, 강좌, 장바구니, 주문·결제, 수강 및 학습 진도를 관리하는 Spring Boot 백엔드입니다.

## 기술 스택

- Java 17, Spring Boot 3.4
- Spring Data JPA, QueryDSL, MySQL 8
- Spring Security, JWT
- Flyway
- MinIO, AWS S3
- Gradle

## 로컬 실행

### 준비물

- JDK 17
- Docker와 Docker Compose

### 실행 절차

1. 환경 변수 예시를 복사하고 로컬 값으로 채웁니다.

   ```bash
   cp .env.template .env
   ```

2. MySQL과 MinIO를 실행합니다.

   ```bash
   docker compose up -d mysql minio minio-init
   ```

3. 애플리케이션을 실행합니다. 기본 활성 프로필은 `local`입니다.

   ```bash
   ./gradlew bootRun
   ```

애플리케이션은 기본적으로 `http://localhost:8080`에서 실행됩니다. API 호출 예시는 [`http/`](http/)에서 확인할 수 있습니다.

## 검증

```bash
./gradlew test
./gradlew build
```

## 문서

- [문서 안내](docs/README.md)
- [아키텍처 개요](docs/architecture/overview.md)
- [구매 흐름](docs/architecture/purchase-flow.md)
- [Architecture Decision Records](docs/adr/README.md)
- [트러블슈팅](docs/troubleshooting/README.md)

