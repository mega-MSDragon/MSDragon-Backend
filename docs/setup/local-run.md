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

## Docker Compose 실행

```bash
cp .env.example .env
docker compose up -d --build
```

확인:

```bash
curl http://localhost/health
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

## 인증 환경 변수

로컬 기본값으로 실행할 수 있지만, 운영 환경에서는 아래 값을 반드시 설정합니다.

| Name | 설명 |
|------|------|
| `APP_AUTH_JWT_SECRET` | 서비스 access/signup JWT 서명 secret. 32바이트 이상 필요 |
| `APP_AUTH_APPLE_CLIENT_ID` | Apple identity token의 audience 검증에 사용할 client id 또는 iOS Bundle ID |

`APP_AUTH_APPLE_CLIENT_ID`가 비어 있으면 Apple 로그인 요청은 설정 오류로 실패합니다. 카카오 로그인은 앱에서 받은 access token으로 Kakao user info API를 호출합니다.

---

## DB 설정

`local` profile은 H2 인메모리 DB를 사용합니다.

| Name | Value |
|------|-------|
| JDBC URL | `jdbc:h2:mem:msdragon` |
| username | `sa` |
| H2 Console | `/h2-console` |

---

## PostgreSQL 전환

PostgreSQL datasource 설정을 추가할 때는 `src/main/resources/application.yaml`에 profile 또는 주석 예시를 남기고, 이 문서에 로컬 실행 방법을 함께 기록합니다.
