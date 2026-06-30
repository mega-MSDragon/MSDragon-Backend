# Database Schema

DB 스키마와 공통 엔티티 규칙을 기록합니다.

---

## 현재 상태

인증/가족 도메인 Entity가 도입되었습니다.

| Entity | Table | 설명 |
|--------|-------|------|
| `User` | `users` | 소셜 로그인 사용자와 회원가입 완료 프로필 |
| `UserRefreshToken` | `user_refresh_tokens` | 해시로 저장하는 refresh token 세션 |
| `Family` | `families` | 대표 자녀 기준 가족 |
| `FamilyMember` | `family_members` | 가족 구성원 |
| `FamilyCode` | `family_codes` | 사용자별 고정 가족 초대 코드 |
| `FamilyCodeUsage` | `family_code_usages` | 가족 코드 매칭 이력 |

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
- `platform`은 요청 앱 플랫폼(`ios`, `android`, `web`)을 선택적으로 기록합니다.
- `device_id`는 현재 요구사항에서 사용하지 않아 두지 않습니다. 기기별 세션 관리가 필요해질 때 추가합니다.

---

## 가족 테이블 구현 메모

### families

- `owner_user_id`는 대표 자녀 사용자입니다.
- 가족은 자녀 쪽 가족을 기준으로 생성합니다.

### family_members

- `user_id`는 unique입니다. 한 사용자는 하나의 가족에만 속합니다.
- 가족당 자녀 1명, 부모 최대 2명 제약은 서비스에서 검증합니다.
- 운영 DB에서는 DBML/ERD 설계처럼 자녀 1명 partial unique index와 부모 최대 2명 trigger를 추가 검토합니다.

### family_codes

- 사용자별 고정 코드입니다.
- 코드 형식은 `MSH-0000` 패턴입니다.
- `code`는 unique이며 생성 시 충돌을 재시도합니다.

### family_code_usages

- 누가 어떤 가족 코드를 입력해 어떤 가족과 매칭됐는지 기록합니다.
- `(family_code_id, requester_user_id)`는 unique입니다.

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
