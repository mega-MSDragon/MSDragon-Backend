# Deploy

EC2 한 대에서 Docker Compose로 Spring Boot, PostgreSQL, Nginx를 함께 실행합니다.

---

## 구성

| Component | Role | Public |
|-----------|------|--------|
| `nginx` | 외부 HTTP 요청을 Spring Boot로 프록시 | `80` |
| `app` | Spring Boot 애플리케이션 | 내부 네트워크 |
| `postgres` | PostgreSQL DB | 내부 네트워크 |

Spring Boot `8080`과 PostgreSQL `5432`는 인터넷에 직접 개방하지 않습니다.

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
| `80` | `0.0.0.0/0` | Cloudflare 또는 HTTP 요청 |

Cloudflare를 사용할 때는 가능하면 `80` source를 Cloudflare IP 대역으로 제한합니다.

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
docker compose up -d --build
```

확인:

```bash
docker compose ps
curl http://localhost/health
```

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

---

## Cloudflare

1. 도메인의 DNS를 Cloudflare로 연결합니다.
2. `A` 레코드를 EC2 public IP로 설정하고 proxy를 켭니다.
3. SSL/TLS 모드는 빠른 MVP라면 `Flexible`로 시작할 수 있습니다.
4. 운영 보안을 올릴 때는 origin certificate을 EC2/Nginx에 설치하고 `Full (strict)`로 전환합니다.

`Flexible`은 사용자와 Cloudflare 사이만 HTTPS이고 Cloudflare와 EC2 사이 통신은 HTTP입니다.
최소 배포에는 단순하지만, 로그인/결제/개인정보가 들어가기 시작하면 `Full (strict)`로 바꿉니다.

---

## 선택 보안 설정

초기 API 확인이 끝난 뒤 Swagger를 외부에서 닫고 싶으면 `.env`에 아래 값을 추가하고 재배포합니다.

```bash
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```
