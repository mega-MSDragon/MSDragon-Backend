# Local Run

로컬 실행 방법과 기본 개발 환경 설정을 정리합니다.

---

## 실행

```bash
./gradlew bootRun
```

기본 포트는 `8080`입니다.

---

## 테스트

```bash
./gradlew test
```

---

## 주요 URL

| Name | URL |
|------|-----|
| Application | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| H2 Console | H2 Console 설정 추가 후 기록 |
| Health Check | `http://localhost:8080/health` |

---

## DB 설정

현재 `build.gradle.kts`에는 H2와 PostgreSQL driver 의존성이 포함되어 있습니다.
실제 datasource/profile 설정이 추가되면 `src/main/resources/application.yaml`과 이 문서를 함께 갱신합니다.

---

## PostgreSQL 전환

PostgreSQL datasource 설정을 추가할 때는 `src/main/resources/application.yaml`에 profile 또는 주석 예시를 남기고, 이 문서에 로컬 실행 방법을 함께 기록합니다.
