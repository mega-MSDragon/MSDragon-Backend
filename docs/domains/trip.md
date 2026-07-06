# Trip Domain

Trip 도메인은 여행 생성의 기본 뼈대와 가족별 여행 조회를 담당합니다.

---

## 책임

- 자녀 사용자가 여행 대상 부모 후보를 조회합니다.
- 서버 고정 여행 도시 catalog를 제공합니다.
- 자녀 사용자가 부모, 도시, 날짜를 선택해 여행 기본 정보를 생성합니다.
- 여행 생성 시 참여자와 여행 일자를 함께 생성합니다.
- 같은 가족 구성원이 여행 목록과 상세를 조회합니다.
- 같은 가족의 날짜가 겹치는 여행 생성을 막습니다.

---

## 패키지 구조

```text
trip
├── controller
├── dto
├── entity
├── repository
└── service
```

---

## 관련 테이블

- `trips`
- `trip_participants`
- `trip_days`
- `families`
- `family_members`
- `parent_profiles`

---

## 관련 API

- `GET /api/v1/trips/parent-candidates`
- `GET /api/v1/trips/destinations`
- `GET /api/v1/trips`
- `GET /api/v1/trips/{tripId}`
- `POST /api/v1/trips`

---

## 구현 결정

- 여행 생성은 현재 자녀 사용자만 허용합니다.
- 부모 상세 프로필이 `completed`인 부모만 여행 대상으로 선택할 수 있습니다.
- 도시 목록은 DB 마스터 테이블 없이 `TripDestinationCode` enum catalog로 제공합니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 여행 생성 직후 상태는 `planning`입니다.
- 실제 코스 추천, 방문지, 경로 계산은 후속 도메인 작업에서 붙입니다.
