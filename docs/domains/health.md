# Health Domain

Health 도메인은 서버 실행 상태를 확인하는 기본 기능입니다.

---

## 책임

- 서버 상태를 `UP`으로 반환합니다.
- 로드밸런서, 배포 확인, 로컬 실행 확인에 사용할 수 있는 최소 상태 API를 제공합니다.

---

## 패키지 구조

```text
health
├── controller
├── dto
└── service
```

---

## 관련 API

- `GET /health`
