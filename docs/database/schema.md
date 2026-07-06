# Database Schema

DB 스키마와 공통 엔티티 규칙을 기록합니다.

---

## 현재 상태

인증/가족/부모 프로필/여행 도메인 Entity가 도입되었습니다.
`docs/database/schema.dbml`과 `docs/database/erd.md`는 PDF 확정 와이어프레임 기준 목표 설계를 포함하므로,
현재 구현 Entity보다 앞서 있는 테이블과 컬럼이 있을 수 있습니다.

| Entity | Table | 설명 |
|--------|-------|------|
| `User` | `users` | 소셜 로그인 사용자와 회원가입 완료 프로필 |
| `UserRefreshToken` | `user_refresh_tokens` | 해시로 저장하는 refresh token 세션 |
| `Family` | `families` | 대표 자녀 기준 가족 |
| `FamilyMember` | `family_members` | 가족 구성원 |
| `FamilyCode` | `family_codes` | 사용자별 고정 가족 초대 코드 |
| `FamilyCodeUsage` | `family_code_usages` | 가족 코드 매칭 이력 |
| `ParentProfile` | `parent_profiles` | 부모님 상세 프로필과 추천용 여행 MBTI 현재값 |
| `ParentProfile.travelThemes` | `parent_profile_themes` | 부모님 프로필별 선호 여행 테마 enum code |
| `Trip` | `trips` | 여행 기본 정보 |
| `TripParticipant` | `trip_participants` | 여행 참여자 |
| `TripDay` | `trip_days` | 여행 일자 |

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
- `relation_label` 컬럼은 남겨두지만 현재 가족 매칭 API에서는 입력받지 않습니다. 가족 응답의 `relationLabel`은 부모 `users.gender` 기준으로 서버에서 계산합니다.
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

## 부모 프로필 테이블 구현 메모

### parent_profiles

- `user_id`는 unique입니다. 부모 사용자 1명은 부모님 상세 프로필을 최대 1개만 가집니다.
- `status`는 `draft`, `completed`를 사용합니다.
- 단계별 저장을 위해 `current_step`, `completion_percent`를 저장합니다.
- 하루 이동 성향은 `walking_pace`에 `slow`, `normal`, `fast` 중 하나로 저장합니다.
- 음식 취향은 `food_preference`에 `korean`, `familiar`, `adventurous` 중 하나로 저장합니다.
- 이동 도움 필요 여부는 `needs_mobility_assistance`에 저장합니다. 코스 추천 시 무장애 정보 가중치로 사용합니다.
- MVP 구현은 추천용 여행 MBTI 현재값 1개를 `personality_type`에 저장합니다.
- 여행 MBTI 타입과 가중치 계산 정책은 `docs/policy/parent-travel-mbti.md`를 기준으로 합니다.
- 상세 MBTI 이력과 점수 테이블은 DBML에 후속 설계로 남겨두고 아직 Entity로 구현하지 않았습니다.

### parent_profile_themes

- `parent_profile_id`, `theme_code` 조합은 unique입니다.
- `theme_code`는 `nature_scenery`, `history_culture`, `shopping`, `activity`, `culture_life`, `landmark`, `experience` 중 하나입니다.
- 현재 구현은 별도 마스터 테이블 FK 대신 enum code를 직접 저장합니다.
- 여행 테마 최소 1개, 최대 3개 제약은 프로필 완료 시 서비스에서 검증합니다.

---

## 여행 테이블 구현 메모

### trips

- `family_id`는 여행을 소유한 가족입니다.
- `created_by_user_id`는 여행을 생성한 자녀 사용자입니다.
- MVP 구현은 `travel_destinations` 마스터 테이블 FK 대신 `destination_code` enum 문자열을 저장합니다.
- `destination_code`는 `daegu`, `gangneung_sokcho`, `gyeongju`, `busan`, `yeosu`, `incheon`, `jeonju`, `jeju`, `seoul`, `suwon_yongin`, `tongyeong_geoje_namhae`, `pohang_andong` 중 하나입니다.
- 여행 생성 직후 `status`는 `planning`입니다.
- 여행 생성 시 `recommendation_snapshot`에 부모 프로필 추천 입력값을 JSON 문자열로 저장합니다.
  현재 스냅샷에는 정책 버전, 도시/날짜, 부모별 `walkingPace`, `needsMobilityAssistance`, `travelThemes`, `foodPreference`, `personalityType`을 포함합니다.
- 같은 가족에서 날짜가 겹치는 여행은 서비스에서 생성 거부합니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.

### trip_participants

- `trip_id`, `user_id` 조합은 unique입니다.
- 여행 생성 시 생성 자녀와 선택한 부모를 참여자로 저장합니다.
- 부모는 최대 2명까지 선택할 수 있습니다.

### trip_days

- 여행 시작일과 종료일을 기준으로 날짜 수만큼 `trip_days` row를 생성합니다.
- `trip_id`, `day_number` 조합은 unique입니다.
- 방문지(`trip_stops`)와 경로(`trip_route_segments`)는 후속 작업에서 연결합니다.

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
