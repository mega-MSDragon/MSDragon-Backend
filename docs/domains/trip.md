# Trip Domain

Trip 도메인은 여행 생성의 기본 뼈대와 가족별 여행 조회를 담당합니다.

---

## 책임

- 자녀 사용자가 여행 대상 부모 후보를 조회합니다.
- 서버 고정 여행 도시 catalog를 제공합니다.
- 자녀 사용자가 부모, 도시, 날짜를 선택해 여행 기본 정보를 생성합니다.
- 여행 생성 시 선택한 부모의 추천 입력값을 스냅샷으로 저장합니다.
- 여행 생성 시 참여자와 여행 일자를 함께 생성합니다.
- 같은 가족 구성원이 여행 목록과 상세를 조회합니다.
- 같은 가족의 날짜가 겹치는 여행 생성을 막습니다.
- 같은 가족 구성원이 일자별 방문지 코스를 조회하고 전체 저장합니다.
- TourAPI 장소/무장애 정보를 조회해 부모 프로필 기반 추천 코스를 생성합니다.
- 코스 편집 화면에서 TourAPI 기반 방문지 검색과 상세 조회를 제공합니다.
- 코스 방문지는 외부 장소 원본이 바뀌어도 여행 당시 코스를 유지할 수 있도록 장소 스냅샷을 저장합니다.

---

## 패키지 구조

```text
trip
├── config
├── controller
├── dto
├── entity
├── repository
├── service
└── tourapi
```

---

## 관련 테이블

- `trips`
- `trip_participants`
- `trip_days`
- `trip_stops`
- `families`
- `family_members`
- `parent_profiles`

---

## 관련 API

- `GET /api/v1/trips/parent-candidates`
- `GET /api/v1/trips/destinations`
- `GET /api/v1/trips`
- `GET /api/v1/trips/{tripId}`
- `GET /api/v1/trips/{tripId}/course`
- `GET /api/v1/trips/{tripId}/places/search`
- `GET /api/v1/trips/{tripId}/places/{contentId}`
- `POST /api/v1/trips/{tripId}/course/recommendation`
- `POST /api/v1/trips`
- `PUT /api/v1/trips/{tripId}/course`

---

## 구현 결정

- 여행 생성은 현재 자녀 사용자만 허용합니다.
- 부모 상세 프로필이 `completed`인 부모만 여행 대상으로 선택할 수 있습니다.
- 도시 목록은 DB 마스터 테이블 없이 `TripDestinationCode` enum catalog로 제공합니다.
- 부모 프로필 스냅샷은 `trips.recommendation_snapshot`에 JSON 문자열로 저장하고 상세 응답에서 구조화해 내려줍니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 여행 생성 직후 상태는 `planning`입니다.
- 코스 저장은 현재 같은 가족 구성원이면 가능합니다. 역할별 편집 제한은 후속 정책으로 좁힐 수 있습니다.
- 코스 저장은 요청 배열 순서대로 `sort_order`를 부여하고 기존 코스를 전체 덮어씁니다.
- 방문지는 현재 `places` 마스터 FK 없이 `trip_stops`에 장소 스냅샷을 저장합니다. 실제 공공데이터/Tmap 연동 후 마스터 캐시가 필요해지면 `places`와 연결합니다.
- 추천 코스 생성은 TourAPI `KorWithService2`의 `areaBasedList2`, `detailCommon2`, `detailWithTour2`를 사용합니다.
- 코스 편집용 방문지 검색은 TourAPI `searchKeyword2`를 사용하고, 상세 조회는 `detailCommon2`, `detailWithTour2`를 사용합니다.
- 추천 생성은 기존 코스를 삭제하고 새 추천 결과를 `trip_stops`에 저장합니다.
- 추천 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`로 변경합니다.
- 현재 추천 생성은 장소 선정까지만 수행하며, Tmap 경로/거리/소요시간 계산은 후속 작업에서 붙입니다.
