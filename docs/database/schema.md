# Database Schema

DB 스키마와 공통 엔티티 규칙을 기록합니다.

---

## 현재 상태

인증 도메인 Entity가 도입되었습니다.

| Entity | Table | 설명 |
|--------|-------|------|
| `User` | `users` | 소셜 로그인 사용자와 회원가입 완료 프로필 |
| `UserRefreshToken` | `user_refresh_tokens` | 해시로 저장하는 refresh token 세션 |

---

## 공통 엔티티 기반

생성/수정 시간 추적이 필요한 JPA Entity에는 `BaseTimeEntity`를 사용합니다.
현재 구현은 JPA Auditing 설정 없이 `@PrePersist`, `@PreUpdate`로 시간을 기록합니다.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `createdAt` | `LocalDateTime` | false | 생성 시간 |
| `updatedAt` | `LocalDateTime` | false | 수정 시간 |

---

## 인증 테이블 구현 메모

### users

- `oauth_provider + oauth_subject`는 unique입니다.
- DB 저장 enum 값은 DBML과 맞춰 소문자 문자열을 사용합니다.
- 회원가입 전 사용자는 저장하지 않습니다. 소셜 로그인 후 `signupToken`을 발급하고, 회원가입 완료 시 `users` row를 생성합니다.

### user_refresh_tokens

- refresh token 원문은 저장하지 않고 SHA-256 해시를 저장합니다.
- 재발급 시 기존 refresh token은 `revoked_at`을 기록하고 새 refresh token을 발급합니다.

---

## 로컬 DB

로컬 profile은 H2 인메모리 DB를 사용합니다.

- JDBC URL: `jdbc:h2:mem:msdragon`
- username: `sa`
- H2 Console path: `/h2-console`

---

## Entity 추가 시 기록할 항목

- 테이블명
- 컬럼명, 타입, nullable 여부
- 인덱스와 unique 제약
- 연관관계
- 공통 timestamp/base entity 적용 여부
