# Package Convention

이 프로젝트는 기능별 레이어드 패키지 구조를 사용합니다.

---

## 기본 구조

```text
com.msdragon.backend
├── common
└── {domain}
    ├── controller
    ├── service
    ├── repository
    ├── entity
    └── dto
```

---

## 레이어 책임

| Package | Responsibility |
|---------|----------------|
| `controller` | HTTP 요청/응답, Swagger 문서화 |
| `service` | 비즈니스 로직 |
| `repository` | DB 접근 |
| `entity` | JPA Entity |
| `dto` | 요청/응답 DTO |
| `common` | 공통 응답, 예외, 설정, 기반 클래스 |

---

## 규칙

- 새 기능은 우선 기능 도메인 패키지 아래에 만듭니다.
- 컨트롤러에는 복잡한 비즈니스 로직을 두지 않습니다.
- Entity를 API 응답으로 직접 반환하지 않고 DTO를 사용합니다.
- 새 최상위 도메인 패키지가 생기면 `docs/domains.md`를 갱신합니다.
