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

Compose로 실행하면 PostgreSQL도 host port로 publish됩니다. 기본 포트는 `.env`의 `POSTGRES_HOST_PORT=5432`이며, 로컬에 이미 PostgreSQL이 떠 있으면 다른 포트로 바꿉니다.

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
| `APP_AUTH_ACCESS_TOKEN_EXPIRATION` | Access token 만료 시간. ISO-8601 Duration, 개발 테스트 기본값 `P365D` |
| `APP_AUTH_APPLE_CLIENT_ID` | Apple identity token의 audience 검증에 사용할 client id 또는 iOS Bundle ID |

`APP_AUTH_APPLE_CLIENT_ID`가 비어 있으면 Apple 로그인 요청은 설정 오류로 실패합니다. 카카오 로그인은 앱에서 받은 access token으로 Kakao user info API를 호출합니다.
현재 access token 기본값은 공모전 개발 테스트를 위해 365일로 길게 설정되어 있습니다. 운영 정책을 적용할 때는 `APP_AUTH_ACCESS_TOKEN_EXPIRATION=PT1H`처럼 줄입니다.

---

## 외부 API 환경 변수

여행 추천 코스와 홈 추천 콘텐츠를 조회하려면 한국관광공사 TourAPI 서비스키가 필요합니다.
같은 키에 `한국관광공사_무장애 여행 정보`와 `한국관광공사_국문 관광정보 서비스_GW` 활용신청이 모두 승인되어 있어야 합니다.
서비스키가 비어 있으면 애플리케이션은 실행됩니다. 추천 코스 생성 API는 설정 오류로 실패하고, 홈 API는 도시 이미지를 `null`, 축제를 빈 목록으로 반환합니다.
여행 경로 최적화 API를 호출하려면 Tmap 앱키가 필요합니다.
Tmap 앱키가 비어 있으면 애플리케이션은 실행되지만, 경로 최적화 API는 설정 오류로 실패합니다.
여행 모드 AI 챗봇을 호출하려면 OpenAI API 키가 필요합니다.
OpenAI API 키가 비어 있어도 애플리케이션은 실행되지만, 질문 전송 API는 설정 오류로 실패합니다.

| Name | 설명 |
|------|------|
| `TOUR_API_SERVICE_KEY` | 한국관광공사 무장애 여행 정보와 국문 관광정보 서비스에 사용할 TourAPI 서비스키 |
| `TOUR_API_MOBILE_APP` | TourAPI `MobileApp` 파라미터. 기본값 `MSDragon` |
| `TOUR_API_CONNECT_TIMEOUT` | TourAPI 연결 timeout. ISO-8601 Duration, 기본값 `PT5S` |
| `TOUR_API_REQUEST_TIMEOUT` | TourAPI 요청 timeout. ISO-8601 Duration, 기본값 `PT10S` |
| `TMAP_APP_KEY` | Tmap 경로 최적화·지오코딩·주변 병원/약국 조회 API 앱키 |
| `TMAP_SEARCH_OPTION` | Tmap 경로 탐색 옵션. 기본값 `0` |
| `TMAP_CAR_TYPE` | Tmap 차량 타입. 기본값 `1` |
| `TMAP_DEFAULT_START_TIME` | 일자별 경로 최적화 기본 출발 시간. 기본값 `10:00` |
| `TMAP_CONNECT_TIMEOUT` | Tmap 연결 timeout. ISO-8601 Duration, 기본값 `PT5S` |
| `TMAP_REQUEST_TIMEOUT` | Tmap 요청 timeout. ISO-8601 Duration, 기본값 `PT15S` |
| `OPENAI_API_KEY` | OpenAI 프로젝트 API 키. Git에 커밋하지 않음 |
| `OPENAI_MODEL` | AI 챗봇 모델. 기본값 `gpt-5.6-luna` |
| `OPENAI_CONNECT_TIMEOUT` | OpenAI 연결 timeout. ISO-8601 Duration, 기본값 `PT5S` |
| `OPENAI_REQUEST_TIMEOUT` | OpenAI 요청 timeout. ISO-8601 Duration, 기본값 `PT30S` |
| `OPENAI_MAX_OUTPUT_TOKENS` | 답변 최대 output token. 기본값 `800` |

---

## 공중화장실 CSV 적재

Docker Compose의 PostgreSQL에 CP949 공중화장실 CSV를 한 번 적재할 때 사용합니다.

```bash
mkdir -p data
cp '/Users/me/Downloads/공중화장실정보.csv' data/public-restrooms.csv

docker compose run --rm \
  -v "$PWD/data:/data" \
  app \
  --spring.main.web-application-type=none \
  --app.restroom-import.enabled=true \
  --app.restroom-import.file=/data/public-restrooms.csv
```

성공한 시설은 100건씩 저장합니다. 작업이 중단되면 같은 명령을 다시 실행하고,
좌표 변환 실패 항목은 `data/public-restrooms-failures.csv`에서 확인합니다.

---

## DB 설정

`local` profile은 H2 인메모리 DB를 사용합니다.

| Name | Value |
|------|-------|
| JDBC URL | `jdbc:h2:mem:msdragon` |
| username | `sa` |
| H2 Console | `/h2-console` |

Docker Compose의 PostgreSQL에 직접 접속할 때는 아래 값을 사용합니다.

| Name | Value |
|------|-------|
| Host | `localhost` |
| Port | `.env`의 `POSTGRES_HOST_PORT` |
| Database | `.env`의 `POSTGRES_DB` |
| Username | `.env`의 `POSTGRES_USER` |
| Password | `.env`의 `POSTGRES_PASSWORD` |

---

## PostgreSQL 전환

PostgreSQL datasource 설정을 추가할 때는 `src/main/resources/application.yaml`에 profile 또는 주석 예시를 남기고, 이 문서에 로컬 실행 방법을 함께 기록합니다.
