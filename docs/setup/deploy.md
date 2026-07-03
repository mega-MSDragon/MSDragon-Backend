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

```env
APP_AUTH_JWT_SECRET=<openssl rand -base64 48 결과값>
APP_AUTH_APPLE_CLIENT_ID=com.msdragon.ios
```

`APP_AUTH_JWT_SECRET`은 의미 있는 단어가 아니라 충분히 긴 랜덤 문자열이어야 합니다. 이 값을 변경하면 기존 로그인 토큰은 모두 무효화됩니다.
`POSTGRES_HOST_PORT`는 EC2 host에서 publish할 PostgreSQL 포트입니다. DBeaver에서 직접 접속하려면 기본값 `5432`를 사용하고, 보안 그룹에서도 같은 포트를 허용합니다.

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
- `.env`에 `APP_AUTH_JWT_SECRET`, `APP_AUTH_APPLE_CLIENT_ID` 설정 완료
- `deploy/nginx/certs/origin.pem` 존재
- `deploy/nginx/certs/origin.key` 존재

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
