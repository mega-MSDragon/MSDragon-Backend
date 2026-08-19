# Trip Domain

Trip 도메인은 여행 생성의 기본 뼈대와 가족별 여행 조회를 담당합니다.

---

## 책임

- 자녀 사용자가 여행 대상 부모 후보를 조회합니다.
- 부모 후보에 상세 프로필 작성 단계와 현재 여행 MBTI 결과 표시 정보를 제공합니다.
- 서버 고정 여행 도시 catalog를 제공합니다.
- 자녀 사용자가 부모, 도시, 날짜, 15자 이하 제목을 선택해 여행 기본 정보를 생성합니다.
- 여행을 만든 자녀가 수정 가능 상태에서 제목, 기간, 참여 부모를 수정합니다. 도시는 생성 후 고정합니다.
- 여행을 만든 자녀가 상태와 관계없이 여행을 soft delete하고 진행 중 여행을 수동 종료합니다.
- 여행 생성 시 선택한 부모의 추천 입력값을 스냅샷으로 저장합니다.
- 여행 생성 시 참여자와 여행 일자를 함께 생성합니다.
- 같은 가족 구성원이 여행 목록과 상세를 조회합니다.
- 가족 연결이 해제된 뒤에도 완료·중단 여행의 기존 참여자는 여행 상세와 코스를 조회할 수 있습니다.
- 같은 가족의 날짜가 겹치는 여행 생성을 막습니다.
- 같은 가족 구성원이 일자별 방문지 코스를 조회하고, 여행을 만든 자녀가 코스를 저장·추천 재생성·경로 최적화합니다.
- 같은 가족 구성원에게 현재 일차와 전체 코스를 포함한 여행 모드를 제공합니다.
- 서울 날짜 기준으로 시작한 여행을 `in_progress`, 종료된 여행을 `completed`로 동기화합니다.
- TourAPI 장소/무장애 정보를 조회해 부모 프로필 기반 추천 코스를 생성합니다.
- 코스 편집 화면에서 TourAPI 기반 부모 프로필 추천, 방문지 검색과 상세 조회를 제공합니다.
- Tmap 경유지 순서 최적화로 일자별 방문 순서, 도착시간, 지도 polyline을 계산합니다.
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
- `GET /api/v1/trips/{tripId}/travel-mode`
- `GET /api/v1/trips/{tripId}/places/search`
- `GET /api/v1/trips/{tripId}/places/{contentId}`
- `POST /api/v1/trips/{tripId}/course/recommendation`
- `POST /api/v1/trips/{tripId}/days/{dayNumber}/route-optimization`
- `POST /api/v1/trips`
- `POST /api/v1/trips/{tripId}/stop`
- `PUT /api/v1/trips/{tripId}`
- `PUT /api/v1/trips/{tripId}/course`
- `DELETE /api/v1/trips/{tripId}`

---

## 구현 결정

- 여행 생성은 현재 자녀 사용자만 허용합니다.
- 부모 상세 프로필이 `completed`인 부모만 여행 대상으로 선택할 수 있습니다.
- 부모 후보 응답의 작성 중 프로필은 `profileCurrentStep`, `profileCompletionPercent`로 표시하고, 완료 프로필은 `personalityResult`로 기존 여행 MBTI 유형명과 문구를 제공합니다.
- 도시 목록은 DB 마스터 테이블 없이 `TripDestinationCode` enum catalog로 제공합니다. 노출 순서와 `Hot!` 배지도 서버 enum에서 관리합니다.
- 여행 제목은 필수이며 최대 15자이고 생성 후에도 수정할 수 있습니다. 도시는 생성 후 변경할 수 없습니다.
- 부모 프로필 스냅샷은 `trips.recommendation_snapshot`에 JSON 문자열로 저장하고 상세 응답에서 구조화해 내려줍니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 캘린더는 별도 API를 두지 않습니다. 클라이언트가 `GET /api/v1/trips`에서 `stopped`, `archived`를 제외한 여행 기간을 표시하고, 서버는 생성 시 날짜 중복을 다시 검증합니다.
- 미래 여행의 생성 직후 상태는 `planning`입니다.
- 시작일이 오늘인 여행은 생성 응답부터 `in_progress`이며, 미래 여행도 시작일이 되면 준비 여부와 관계없이 `in_progress`로 변경합니다.
- 종료일 다음 날부터 `completed`로 변경합니다. 상태 전환은 서울 날짜를 기준으로 여행 상태를 사용하는 요청에서 수행하며 별도 스케줄러를 두지 않습니다.
- 여행 모드는 시작일 00:00부터 종료일 23:59까지 같은 가족 구성원 모두가 접근할 수 있습니다. 여행 참여자로 선택되지 않은 가족 구성원도 포함합니다.
- 여행 기본정보 수정은 생성한 자녀만 할 수 있으며 모든 수정 가능 상태에서 제목, 기간, 참여 부모를 변경할 수 있습니다.
- 여행 중 변경한 기간에는 서울 기준 오늘이 포함되어야 합니다. `completed`, `stopped`, `archived` 상태에서는 수정할 수 없습니다.
- 날짜 또는 참여 부모를 바꾸면 현재 부모 프로필로 추천 스냅샷을 다시 만듭니다. 같은 일차의 기존 코스와 경로는 유지합니다.
- 날짜 변경 시 기존 `trip_days`를 일차 번호 기준으로 유지합니다. 줄어든 뒤쪽 일차만 삭제하고 늘어난 일차는 빈 일정으로 추가하며, 다른 여행과 날짜가 겹치면 수정하지 않습니다.
- 기간 또는 참여 부모를 여행 중 변경하면 저장 후 상태는 `in_progress`를 유지합니다.
- 기간 또는 참여 부모가 바뀌면 기존 부모 평가 요청, 피드백, 효도 리포트를 삭제합니다.
- 코스 저장, 추천 재생성, 경로 최적화는 여행을 만든 자녀만 `planning`, `ready`, `in_progress` 상태에서 할 수 있습니다.
- 생성 자녀는 여행 상태와 관계없이 soft delete할 수 있고 삭제 시 `deleted_at`을 기록합니다.
- `in_progress` 여행은 생성 자녀가 수동 종료할 수 있으며 정상 종료와 동일하게 상태를 `completed`로 바꾸고 기록·평가 데이터를 유지합니다.
- `completed`, `stopped`, `archived` 여행의 기본정보와 코스는 변경할 수 없습니다.
- 방문지 추가·삭제·순서 변경은 단건 API 없이 클라이언트에서 임시 편집한 최종 코스를 `PUT /api/v1/trips/{tripId}/course`로 전체 저장합니다. 요청에서 빠진 일자는 빈 코스가 됩니다.
- 방문지 메모는 `PUT /api/v1/trips/{tripId}/stops/{stopId}/note`로 즉시 저장하거나 삭제합니다.
- 코스 저장은 요청 배열 순서대로 `sort_order`를 부여하고 기존 코스를 전체 덮어씁니다.
- 방문지는 현재 `places` 마스터 FK 없이 `trip_stops`에 장소 스냅샷을 저장합니다. 실제 공공데이터/Tmap 연동 후 마스터 캐시가 필요해지면 `places`와 연결합니다.
- 추천 코스 생성은 TourAPI `KorWithService2`의 `areaBasedList2`, `detailCommon2`, `detailWithTour2`를 사용합니다.
- 코스 편집용 추천은 `areaBasedList2`, 검색은 `searchKeyword2`, 상세는 `detailCommon2`, `detailIntro2`, `detailImage2`, `detailWithTour2`를 사용합니다.
- 경로 최적화는 Tmap `routeOptimization10`을 사용하며, 시작점/도착점 입력 없이 모든 시작/끝 조합을 조회해 최단 결과를 선택합니다.
- 추천 생성은 기존 코스를 삭제하고 새 추천 결과를 `trip_stops`에 저장합니다.
- 추천 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`로 변경합니다.
- 현재 추천 생성은 장소 선정까지만 수행합니다. 경로/거리/소요시간은 별도 경로 최적화 API 호출로 계산합니다.
- 확정 생성 화면은 `POST /trips` → `POST /course/recommendation` → 반환된 모든 일자의 `POST /route-optimization`을 동기식으로 호출합니다. 모든 호출이 성공한 뒤 홈 이동을 활성화합니다.
- 별도 코스 생성 job과 진행률 조회 API는 두지 않습니다. 화면의 단계와 퍼센트는 클라이언트가 위 API 호출 상태로 표시합니다.
- 코스 전체 저장은 방문지 구성이 바뀐 `trip_days`의 경로 캐시만 무효화하고, 추천 생성은 모든 일자의 경로 캐시를 무효화합니다.
