# Deploy

EC2 한 대에서 Docker Compose로 Spring Boot, PostgreSQL, Nginx를 함께 실행합니다.

---

## 구성

| Component | Role | Public |
|-----------|------|--------|
| `nginx` | 외부 HTTPS 요청을 Spring Boot로 프록시 | `80`, `443` |
| `app` | Spring Boot 애플리케이션 | 내부 네트워크 |
| `postgres` | PostgreSQL DB | `5432` |

Spring Boot `8080`은 인터넷에 직접 개방하지 않습니다.
PostgreSQL `5432`는 DBeaver 등 DB 클라이언트 접근을 위해 host port로 publish하되, AWS 보안 그룹에서 접근 가능한 IP를 제한합니다.
Cloudflare를 사용할 때는 Cloudflare Origin Certificate를 Nginx에 설치하고 `Full (strict)` 모드를 사용합니다.

---

## 비용 기준

- AWS EC2는 계정 생성 시점과 Free Tier/크레딧 조건에 따라 비용이 달라집니다.
- EC2, EBS 볼륨, 퍼블릭 IPv4, 데이터 전송이 주요 비용 항목입니다.
- Cloudflare DNS/프록시/기본 SSL은 무료 플랜으로 시작할 수 있습니다.

---

## EC2 준비

보안 그룹 인바운드:

| Port | Source | Purpose |
|------|--------|---------|
| `22` | 내 IP | SSH |
| `80` | `0.0.0.0/0` | HTTP 요청을 HTTPS로 리다이렉트 |
| `443` | `0.0.0.0/0` | Cloudflare HTTPS 요청 |
| `5432` | 내 IP 또는 팀원 IP | PostgreSQL 클라이언트 접속 |

Cloudflare를 사용할 때는 가능하면 `80` source를 Cloudflare IP 대역으로 제한합니다.
PostgreSQL `5432`를 `0.0.0.0/0`로 여는 것은 임시 확인 목적에만 사용하고, 가능한 한 `/32` 단위의 허용 IP만 등록합니다.

EC2에 Docker와 Docker Compose plugin을 설치합니다.

```bash
docker --version
docker compose version
```

---

## 서버 최초 배포

```bash
sudo mkdir -p /opt/MSDragon-Backend
sudo chown "$USER:$USER" /opt/MSDragon-Backend
git clone https://github.com/mega-MSDragon/MSDragon-Backend.git /opt/MSDragon-Backend
cd /opt/MSDragon-Backend
cp .env.example .env
vi .env
openssl rand -base64 48
mkdir -p deploy/nginx/certs
vi deploy/nginx/certs/origin.pem
vi deploy/nginx/certs/origin.key
chmod 600 deploy/nginx/certs/origin.pem deploy/nginx/certs/origin.key
docker compose up -d --build
```

확인:

```bash
docker compose ps
curl -k https://localhost/health
```

`.env`의 인증 관련 값:

`APP_AUTH_APPLE_PRIVATE_KEY`는 여러 줄인 `.p8` 파일 내용입니다. `.env`는 여러 줄 값을 그대로 다루기 어려우므로 base64 본문만 한 줄로 넣는 방식을 권장합니다. 서버는 PEM 헤더와 모든 공백을 제거하고 읽으므로 두 형태 모두 동작합니다.

```bash
# .p8에서 한 줄 값 만들기
tr -d '\n' < AuthKey_XXXXXXXXXX.p8 | sed 's/-----[A-Z ]*-----//g'
```


```env
APP_AUTH_JWT_SECRET=<openssl rand -base64 48 결과값>
APP_AUTH_ACCESS_TOKEN_EXPIRATION=P365D
APP_AUTH_APPLE_CLIENT_ID=com.msdragon.ios
APP_AUTH_APPLE_TEAM_ID=<Apple Developer 팀 ID>
APP_AUTH_APPLE_KEY_ID=<Sign in with Apple 키의 Key ID>
APP_AUTH_APPLE_PRIVATE_KEY=<.p8 파일 내용>
APP_AUTH_KAKAO_ADMIN_KEY=<카카오 어드민 키>
APP_AUTH_OAUTH_REQUEST_TIMEOUT=PT5S
TOUR_API_SERVICE_KEY=<한국관광공사 TourAPI 서비스키>
TOUR_API_MOBILE_APP=MSDragon
TOUR_API_CONNECT_TIMEOUT=PT5S
TOUR_API_REQUEST_TIMEOUT=PT10S
TMAP_APP_KEY=<Tmap API 앱키>
TMAP_SEARCH_OPTION=0
TMAP_CAR_TYPE=1
TMAP_DEFAULT_START_TIME=10:00
TMAP_CONNECT_TIMEOUT=PT5S
TMAP_REQUEST_TIMEOUT=PT15S
OPENAI_API_KEY=<새로 발급한 OpenAI 프로젝트 API 키>
OPENAI_MODEL=gpt-5.6-luna
OPENAI_CONNECT_TIMEOUT=PT5S
OPENAI_REQUEST_TIMEOUT=PT30S
OPENAI_MAX_OUTPUT_TOKENS=800
```

`APP_AUTH_JWT_SECRET`은 의미 있는 단어가 아니라 충분히 긴 랜덤 문자열이어야 합니다. 이 값을 변경하면 기존 로그인 토큰은 모두 무효화됩니다.
`APP_AUTH_ACCESS_TOKEN_EXPIRATION`은 ISO-8601 Duration 형식이며, 개발 테스트 기간에는 `P365D`를 사용합니다. 운영 전에는 `PT1H` 등 실제 인증 정책에 맞는 값으로 줄입니다. 설정을 변경해도 기존 토큰의 만료 시각은 바뀌지 않으므로 새 토큰을 발급받아야 합니다.
`POSTGRES_HOST_PORT`는 EC2 host에서 publish할 PostgreSQL 포트입니다. DBeaver에서 직접 접속하려면 기본값 `5432`를 사용하고, 보안 그룹에서도 같은 포트를 허용합니다.
`TOUR_API_SERVICE_KEY`는 같은 키에 한국관광공사 무장애 여행 정보와 국문 관광정보 서비스 활용신청이 모두 승인되어 있어야 합니다.
값이 비어 있으면 서버는 실행되지만 추천 코스 생성 API는 설정 오류로 실패하고, 홈 API는 도시 이미지와 축제를 제외한 축소 응답을 반환합니다.
`TMAP_APP_KEY`가 비어 있으면 서버는 실행되지만 경로 최적화, 공중화장실 좌표 변환, 주변 병원·약국 조회 API는 설정 오류로 실패합니다.
`OPENAI_API_KEY`가 비어 있으면 서버는 실행되지만 여행 모드 AI 질문 전송 API는 설정 오류로 실패합니다.
timeout 값은 Spring Boot가 읽을 수 있는 ISO-8601 Duration 형식으로 작성합니다. 예: `PT10S`.

### OpenAI 키 반영

채팅이나 저장소에 노출한 키는 폐기하고 OpenAI 프로젝트에서 새 키를 발급합니다. EC2의 `/opt/MSDragon-Backend/.env`에만 저장하며 Git, Dockerfile, `application.yaml`, GitHub Actions 로그에는 넣지 않습니다.

```bash
cd /opt/MSDragon-Backend
vi .env
docker compose up -d --build --force-recreate app
docker compose exec app sh -lc 'test -n "$OPENAI_API_KEY" && echo OPENAI_API_KEY=set || echo OPENAI_API_KEY=missing'
docker compose logs app --tail 100
```

현재 배포 workflow는 서버의 `.env`를 그대로 사용하므로 `OPENAI_API_KEY`를 GitHub Repository Secret에 중복 등록할 필요가 없습니다. `.env`를 수정한 뒤에는 app 컨테이너를 다시 생성해야 새 값이 반영됩니다.

---

## 공중화장실 일회성 적재

최신 애플리케이션 이미지를 배포한 뒤 EC2 프로젝트 디렉터리에서 실행합니다.
원본 파일은 Git에 커밋하지 않습니다.

```bash
cd /opt/MSDragon-Backend
mkdir -p data
cp '/업로드한/공중화장실정보.csv' data/public-restrooms.csv

docker compose run --rm \
  -v "$PWD/data:/data" \
  app \
  --spring.main.web-application-type=none \
  --app.restroom-import.enabled=true \
  --app.restroom-import.file=/data/public-restrooms.csv
```

- 기존 `TMAP_APP_KEY`를 사용하므로 별도 환경변수는 필요하지 않습니다.
- 호출 제한이나 API 오류로 중단되면 이미 저장된 시설은 유지됩니다. 다음 날 같은 명령을 다시 실행하면 기존 시설을 건너뛰고 이어서 처리합니다.
- 누락과 실패 항목은 `data/public-restrooms-failures.csv`에 기록됩니다.
- 정상 app 컨테이너에서는 적재기가 실행되지 않습니다.

---

## GitHub Actions 배포

Repository secrets:

| Secret | Example |
|--------|---------|
| `EC2_HOST` | `1.2.3.4` |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | EC2 접속 private key 전체 |
| `DEPLOY_PATH` | `/opt/MSDragon-Backend` |

`main` 브랜치에 push되면 `.github/workflows/deploy.yml`이 EC2에 SSH 접속해서 아래 작업을 수행합니다.

```bash
git fetch origin main
git reset --hard origin/main
docker compose up -d --build
```

자동 배포 전에 EC2에 아래가 먼저 준비되어 있어야 합니다.

- `DEPLOY_PATH` 디렉터리 존재
- 해당 디렉터리에 Git repository clone 완료
- `.env` 파일 존재
- `.env`에 `APP_AUTH_JWT_SECRET`, `APP_AUTH_ACCESS_TOKEN_EXPIRATION`, `APP_AUTH_APPLE_CLIENT_ID` 설정 완료
- 추천 코스와 홈 추천 콘텐츠를 사용할 경우 `.env`에 `TOUR_API_SERVICE_KEY` 설정 완료 및 무장애 여행 정보·국문 관광정보 서비스 활용신청 승인
- 경로 최적화·공중화장실 적재·주변 병원·약국 API를 사용할 경우 `.env`에 `TMAP_APP_KEY` 설정 완료
- 여행 모드 AI 챗봇을 사용할 경우 `.env`에 `OPENAI_API_KEY` 설정 완료
- 탈퇴 시 소셜 연결 해제를 사용할 경우 `.env`에 `APP_AUTH_APPLE_TEAM_ID`, `APP_AUTH_APPLE_KEY_ID`, `APP_AUTH_APPLE_PRIVATE_KEY`, `APP_AUTH_KAKAO_ADMIN_KEY` 설정 완료. 없으면 연결 해제를 건너뛰고 탈퇴는 정상 동작합니다
- `deploy/nginx/certs/origin.pem` 존재
- `deploy/nginx/certs/origin.key` 존재

### 여행 중단 상태 DB 반영

기존 PostgreSQL의 `trips.status` check constraint는 Hibernate `ddl-auto=update`가 enum 값 추가를 반영하지 못할 수 있습니다. `stopped` 상태를 처음 배포할 때 DBeaver 또는 `psql`에서 한 번 실행합니다.

```sql
ALTER TABLE trips DROP CONSTRAINT IF EXISTS trips_status_check;
ALTER TABLE trips
    ADD CONSTRAINT trips_status_check
    CHECK (status IN ('planning', 'ready', 'in_progress', 'completed', 'stopped', 'archived'));
```

기존 constraint 이름이 다르면 아래 쿼리로 이름을 확인한 뒤 해당 constraint를 삭제합니다.

```sql
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'trips'::regclass
  AND contype = 'c';
```

---

## Cloudflare

1. 도메인의 DNS를 Cloudflare로 연결합니다.
2. `A` 레코드를 EC2 public IP로 설정하고 proxy를 켭니다.
3. `SSL/TLS > Origin Server`에서 Origin Certificate를 발급합니다.
4. 발급한 인증서를 EC2의 `deploy/nginx/certs/origin.pem`에 저장합니다.
5. 발급한 private key를 EC2의 `deploy/nginx/certs/origin.key`에 저장합니다.
6. SSL/TLS 모드는 `Full (strict)`로 설정합니다.

Origin Certificate private key는 절대 Git에 커밋하지 않습니다.
`deploy/nginx/certs/`는 `.gitignore`에 포함되어 있습니다.

---

## 선택 보안 설정

초기 API 확인이 끝난 뒤 Swagger를 외부에서 닫고 싶으면 `.env`에 아래 값을 추가하고 재배포합니다.

```bash
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```
