# Family Domain

Family 도메인은 가족 코드 발급과 부모-자녀 가족 매칭을 담당합니다.

---

## 책임

- 사용자별 고정 가족 코드를 발급/조회합니다.
- 상대방 가족 코드로 부모-자녀 관계를 생성합니다.
- 로그인 사용자의 가족과 구성원 프로필을 조회합니다.
- 자녀 1명, 부모 최대 2명 제약을 서비스에서 검증합니다.
- 한 사용자가 여러 가족에 속하지 않도록 검증합니다.
- 매칭 이력을 `family_code_usages`에 기록합니다.

---

## 패키지 구조

```text
family
├── controller
├── dto
├── entity
├── repository
└── service
```

---

## 관련 테이블

- `families`
- `family_members`
- `family_codes`
- `family_code_usages`

---

## 관련 API

- `POST /api/v1/family/code`
- `POST /api/v1/family/matches`
- `GET /api/v1/family`

---

## 후속 범위

- 가족 구성원 프로필 수정 권한 정책
- 가족 초대 코드 만료/재발급 정책이 필요할 경우 코드 상태 전환 API 추가
