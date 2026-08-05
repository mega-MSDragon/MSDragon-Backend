# Trip API

여행 생성 기본 흐름을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증·정책 오류도 HTTP `200`으로 반환하며, 문서의 `400/401/403/404`는 본문 `status`입니다. 외부 API·서버 실패는 실제 HTTP `500`입니다.

---

## 결정 사항

- 여행 생성은 현재 자녀 사용자만 가능합니다.
- 여행 대상은 같은 가족에 연결된 부모만 선택할 수 있습니다.
- 선택한 부모의 상세 프로필이 `completed` 상태여야 여행을 생성할 수 있습니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 같은 가족에서 날짜가 겹치는 여행은 생성할 수 없습니다.
- 도시 목록은 현재 서버 고정 catalog로 내려주고, 여행에는 `destinationCode` 문자열을 저장합니다.
- 여행 생성 시 선택한 부모의 추천 입력값을 `recommendationSnapshot`으로 저장합니다. 이후 부모 프로필이 수정되어도 생성 당시 추천 기준은 유지됩니다.
- 여행 기본정보 수정은 여행을 만든 자녀만 할 수 있습니다. `planning`, `ready`에서는 전체 항목을, `in_progress`에서는 오늘을 포함하는 기간과 참여 부모만 변경할 수 있습니다.
- 여행 중 제목과 도시는 고정입니다. 준비 중 제목만 수정하면 기존 추천 스냅샷과 코스를 유지하고, 도시·날짜·참여 부모를 수정하면 현재 부모 프로필로 추천 스냅샷을 다시 만들고 기존 코스와 경로를 초기화합니다.
- 참여 부모 구성이 바뀌면 저장된 여행 10계명, 문구 10개, 모든 참여자 서명을 함께 삭제하고 처음부터 다시 작성하도록 합니다.
- 여행 기간 또는 참여 부모 구성이 바뀌면 기존 부모 평가 요청, 제출된 피드백, 효도 리포트도 함께 삭제합니다.
- 기존 코스나 경로가 있는 상태에서 추천 입력을 바꾸려면 `courseResetConfirmed=true`를 전달해야 합니다.
- 코스 전체 저장, 추천 재생성, 경로 최적화는 여행을 만든 자녀만 `planning`, `ready`, `in_progress` 상태에서 할 수 있습니다. `completed`, `archived` 여행은 변경할 수 없습니다.
- 여행 코스는 일자별 방문지 목록을 전체 저장합니다. 요청 배열 순서가 방문 순서가 되며, 포함하지 않은 일자는 빈 코스로 저장됩니다.
- 저장된 방문지는 외부 API 원본이 바뀌어도 기존 코스를 유지할 수 있도록 `trip_stops`에 장소 스냅샷을 저장합니다.
- 추천 코스 생성은 한국관광공사 TourAPI 무장애여행 서비스로 장소 후보를 조회하고, 부모 프로필 스냅샷으로 점수를 계산해 `trip_stops`에 저장합니다.
- 추천 코스 생성은 장소 추천까지만 수행합니다. Tmap 경로/거리/소요시간은 별도 경로 최적화 API를 호출해 계산합니다.
- 추천 코스 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`로 변경됩니다.
- 코스 편집 중 방문지 검색/상세 조회는 TourAPI를 조회해 후보를 내려주며, 실제 코스 반영은 클라이언트가 선택한 장소를 `PUT /api/v1/trips/{tripId}/course`로 전체 저장할 때 이루어집니다.
- 방문지 검색/상세 조회에서도 숙박은 제외하고, TourAPI 원본 응답 일부는 코스 저장 시 `sourcePayload`로 보관할 수 있게 내려줍니다.
- 일자별 경로 최적화는 Tmap 경유지 순서 최적화 10 API를 사용합니다. 사용자가 시작점/도착점을 입력하지 않으므로 서버가 모든 시작/도착 조합을 조회해 가장 짧은 결과를 선택합니다.
- 코스 저장 또는 추천 코스 재생성 시 기존 경로 캐시는 무효화됩니다.
- 서울 날짜 기준 시작일이 되면 준비 여부와 관계없이 여행 상태를 `in_progress`, 종료일 다음 날부터 `completed`로 자동 동기화합니다.
- 여행 모드는 시작일 00:00부터 종료일 23:59까지 같은 가족 구성원 모두가 이용할 수 있습니다. 여행 참여자로 선택되지 않은 가족 구성원도 포함합니다.
- 여행 모드 주변 공중화장실은 DB 적재 데이터를 조회하고, 병원·약국은 Tmap POI 주변 카테고리 검색 결과를 실시간으로 조회합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/trips/parent-candidates` | 여행 대상 부모 후보 조회 |
| `GET` | `/api/v1/trips/destinations` | 여행 도시 목록 조회 |
| `GET` | `/api/v1/trips` | 내 가족 여행 목록 조회 |
| `GET` | `/api/v1/trips/{tripId}` | 여행 상세 조회 |
| `GET` | `/api/v1/trips/{tripId}/course` | 여행 코스 조회 |
| `GET` | `/api/v1/trips/{tripId}/travel-mode` | 현재 일차와 전체 코스를 포함한 여행 모드 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-restrooms` | 현재 위치 주변 공중화장실 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-hospitals` | 현재 위치 주변 병원 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-pharmacies` | 현재 위치 주변 약국 조회 |
| `GET` | `/api/v1/trips/{tripId}/places/search` | 코스 편집용 방문지 검색 |
| `GET` | `/api/v1/trips/{tripId}/places/{contentId}` | 코스 편집용 방문지 상세 조회 |
| `POST` | `/api/v1/trips` | 여행 생성 |
| `POST` | `/api/v1/trips/{tripId}/course/recommendation` | 여행 추천 코스 생성 |
| `POST` | `/api/v1/trips/{tripId}/days/{dayNumber}/route-optimization` | 여행 일자 경로 최적화 |
| `PUT` | `/api/v1/trips/{tripId}` | 여행 기본정보 수정 |
| `PUT` | `/api/v1/trips/{tripId}/course` | 여행 코스 전체 저장 |

---

## GET /api/v1/trips/parent-candidates

자녀 사용자가 같은 가족에 연결된 부모와 부모 상세 프로필 완료 여부를 조회합니다.
가족 매칭 전이면 `familyId=null`, `parents=[]`를 반환합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 대상 부모 후보 조회 성공",
  "data": {
    "familyId": 1,
    "parents": [
      {
        "userId": 2,
        "displayName": "김영희",
        "gender": "female",
        "relationLabel": "엄마",
        "profileExists": true,
        "profileCompleted": true,
        "profileStatus": "completed",
        "profileCompletionPercent": 100
      }
    ]
  }
}
```

---

## GET /api/v1/trips/destinations

도시 선택 화면에 표시할 서버 고정 여행 도시 목록을 조회합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 도시 목록 조회 성공",
  "data": [
    {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    }
  ]
}
```

현재 허용 `code`:

`daegu`, `gangneung_sokcho`, `gyeongju`, `busan`, `yeosu`, `incheon`, `jeonju`, `jeju`, `seoul`, `suwon_yongin`, `tongyeong_geoje_namhae`, `pohang_andong`

---

## POST /api/v1/trips

자녀 사용자가 여행 대상 부모, 도시, 날짜를 선택해 여행 기본 정보를 생성합니다.

### Request

```json
{
  "parentUserIds": [2],
  "destinationCode": "gyeongju",
  "startDate": "2026-07-10",
  "endDate": "2026-07-11",
  "title": "경주 가족 여행"
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `parentUserIds` | number array | true | 같은 가족에 연결된 부모 사용자 ID 목록. 최대 2명 |
| `destinationCode` | enum | true | 여행 도시 코드 |
| `startDate` | date | true | 여행 시작일 |
| `endDate` | date | true | 여행 종료일. 시작일과 같거나 이후 |
| `title` | string | false | 최대 80자. 없으면 `{도시명} 여행` |

### Response

```json
{
  "status": 201,
  "success": true,
  "message": "여행 생성 성공",
  "data": {
    "id": 1,
    "familyId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    },
    "startDate": "2026-07-10",
    "endDate": "2026-07-11",
    "status": "planning",
    "participants": [
      {
        "userId": 1,
        "role": "child",
        "displayName": "최혜린",
        "gender": "female",
        "relationLabel": null
      },
      {
        "userId": 2,
        "role": "parent",
        "displayName": "김영희",
        "gender": "female",
        "relationLabel": "엄마"
      }
    ],
    "recommendationSnapshot": {
      "policyVersion": "parent-travel-mbti-v1",
      "capturedAt": "2026-07-06T12:00:00",
      "destinationCode": "gyeongju",
      "startDate": "2026-07-10",
      "endDate": "2026-07-11",
      "parents": [
        {
          "parentUserId": 2,
          "parentProfileId": 1,
          "displayName": "김영희",
          "relationLabel": "엄마",
          "walkingPace": "slow",
          "needsMobilityAssistance": false,
          "travelThemes": ["nature_scenery"],
          "foodPreference": "familiar",
          "personalityType": "healing_traveler",
          "profileCompletedAt": "2026-07-01T12:00:00"
        }
      ]
    },
    "days": [
      {
        "id": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10"
      },
      {
        "id": 2,
        "dayNumber": 2,
        "travelDate": "2026-07-11"
      }
    ]
  }
}
```

미래 여행은 생성 직후 `status=planning`입니다. 시작일이 오늘이면 생성 응답부터 `in_progress`이며, 추천 코스 생성이 완료된 미래 여행은 `ready`가 됩니다.
서울 날짜 기준 시작일부터 `in_progress`, 종료일 다음 날부터 `completed`로 자동 전환합니다.
`recommendationSnapshot.policyVersion`은 부모 여행 MBTI 정책 버전을 의미합니다.

---

## PUT /api/v1/trips/{tripId}

여행을 만든 자녀가 여행 정보 편집 화면의 제목, 도시, 날짜, 참여 부모를 한 번에 저장합니다.
`planning`, `ready`에서는 모든 항목을 수정할 수 있습니다. `in_progress`에서는 제목과 도시를 기존 값 그대로 보내고, 서울 기준 오늘을 포함하는 기간과 참여 부모만 수정할 수 있습니다.
`completed`, `archived` 상태에서는 수정할 수 없습니다.

### Request

```json
{
  "title": "부산 가족 여행",
  "destinationCode": "busan",
  "startDate": "2026-07-15",
  "endDate": "2026-07-17",
  "parentUserIds": [2, 3],
  "courseResetConfirmed": true
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `title` | string | true | 여행 제목. 최대 80자. 여행 중에는 기존 값과 같아야 함 |
| `destinationCode` | enum | true | 여행 도시 코드. 여행 중에는 기존 값과 같아야 함 |
| `startDate` | date | true | 여행 시작일. 여행 중에는 변경 기간에 오늘이 포함되어야 함 |
| `endDate` | date | true | 여행 종료일. 시작일과 같거나 이후이며 여행 중에는 변경 기간에 오늘이 포함되어야 함 |
| `parentUserIds` | number array | true | 같은 가족의 프로필 작성 완료 부모. 1명 이상, 최대 2명. 기존 참여 부모 구성이 바뀌면 10계명과 모든 서명 삭제 |
| `courseResetConfirmed` | boolean | false | 기존 코스가 있는 상태에서 도시, 날짜 또는 참여 부모를 바꿀 때 `true`. 기본값 `false` |

변경 영향:

| 변경 항목 | 기존 코스 | 추천 스냅샷 | 10계명/서명 | 평가 요청/피드백/리포트 | 상태 |
|-----------|-----------|-----------------|-------------|--------------------------|------|
| 제목만 변경 (`planning`, `ready`) | 유지 | 유지 | 유지 | 유지 | 유지 |
| 도시 변경 (`planning`, `ready`) | 삭제 | 현재 부모 프로필로 재생성 | 유지 | 유지 | `planning` |
| 날짜 변경 | 삭제 후 여행 일자 재생성 | 현재 부모 프로필로 재생성 | 유지 | 전체 삭제 | 준비 중이면 `planning`, 여행 중이면 `in_progress` |
| 참여 부모 변경 | 삭제 후 참여자 갱신 | 현재 부모 프로필로 재생성 | 전체 삭제 | 전체 삭제 | 준비 중이면 `planning`, 여행 중이면 `in_progress` |

기존 코스나 경로가 있는데 영향 항목을 바꾸면서 `courseResetConfirmed=false`이면 HTTP `200`, 본문 `status=400`을 반환합니다.
다른 여행과 날짜가 겹치거나, 같은 가족의 작성 완료 부모가 아니면 수정할 수 없습니다.
참여 부모 ID 집합이 실제로 바뀌어 수정이 성공하면 별도 확인값 없이 기존 10계명과 모든 서명이 삭제되며, 자녀부터 다시 작성하고 서명해야 합니다.
날짜 또는 참여 부모가 바뀌면 기존 평가 요청, 부모 피드백, 효도 리포트도 삭제되므로 변경된 조건에서 다시 요청하고 제출해야 합니다.

### Response

`GET /api/v1/trips/{tripId}`와 같은 `TripDetailResponse` 형태입니다.

상세 정책은 `docs/policy/trip-edit.md`를 따릅니다.

---

## GET /api/v1/trips

로그인 사용자가 속한 가족의 여행 목록을 조회합니다.
가족 매칭 전이면 빈 목록을 반환합니다.
조회 시 서울 날짜를 기준으로 시작한 여행은 `in_progress`, 종료된 여행은 `completed`로 상태를 동기화합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "내 가족 여행 목록 조회 성공",
  "data": {
    "familyId": 1,
    "trips": [
      {
        "id": 1,
        "title": "경주 여행",
        "destination": {
          "code": "gyeongju",
          "displayName": "경주",
          "displayOrder": 3,
          "badgeLabel": "인기"
        },
        "startDate": "2026-07-10",
        "endDate": "2026-07-11",
        "status": "planning",
        "participantCount": 2
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}

같은 가족 구성원이 여행 상세를 조회합니다.
다른 가족 여행이면 HTTP `200`, 본문 `status=403`을 반환합니다.
응답 전 여행 상태를 현재 서울 날짜에 맞춰 동기화합니다.

### Response

`POST /api/v1/trips`와 같은 `TripDetailResponse` 형태입니다.

---

## GET /api/v1/trips/{tripId}/course

같은 가족 구성원이 여행 코스를 조회합니다.
저장된 방문지가 없으면 여행 일자별로 `stops=[]`를 반환합니다.
응답 전 여행 상태를 현재 서울 날짜에 맞춰 동기화합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 코스 조회 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    },
    "status": "planning",
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "route": {
          "provider": "tmap",
          "totalDistanceMeters": 6230,
          "totalDurationSeconds": 1757,
          "optimizedAt": "2026-07-07T12:00:00",
          "polyline": [
            {
              "longitude": 129.2247,
              "latitude": 35.8562
            }
          ],
          "sourcePayload": {
            "provider": "tmap",
            "operation": "routeOptimization10",
            "policyVersion": "tmap-route-optimization-v1"
          }
        },
        "stops": [
          {
            "id": 1,
            "sortOrder": 1,
            "stopType": "sightseeing",
            "sourceProvider": "tour_api",
            "externalPlaceId": "988449",
            "contentTypeId": "12",
            "name": "오도리 공원",
            "category": "관광지",
            "address": "대구광역시 동구 효목동",
            "latitude": 35.8821234,
            "longitude": 128.6212345,
            "phone": "053-123-4567",
            "homepageUrl": "https://example.com",
            "imageUrl": "https://example.com/park.jpg",
            "overview": "짧은 산책을 즐기기 좋은 공원입니다.",
            "arrivalTime": "10:30:00",
            "dwellMinutes": 60,
            "note": "부모님과 사진 찍기",
            "recommendationReason": "짧은 산책과 휴식에 적합합니다.",
            "recommendationTags": ["nature_scenery", "low_slope"],
            "sourcePayload": {
              "contentid": "988449",
              "route": "출입구까지 경사로가 설치되어 있음"
            },
            "isManualAdded": false
          }
        ]
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}/travel-mode

같은 가족 구성원이 여행 기간 중 현재 일차와 전체 일자별 코스를 조회합니다.
여행 참여자로 선택되지 않은 같은 가족 구성원도 접근할 수 있습니다.

- 시작일 전: HTTP `200`, 본문 `status=400`
- 시작일 00:00부터 종료일 23:59까지: HTTP `200`, 본문 `status=200`, 여행 상태 `in_progress`
- 종료일 다음 날부터: 여행 상태를 `completed`로 동기화한 뒤 HTTP `200`, 본문 `status=400`
- 다른 가족 사용자: HTTP `200`, 본문 `status=403`

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 모드 조회 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    },
    "startDate": "2026-07-10",
    "endDate": "2026-07-11",
    "status": "in_progress",
    "currentDayNumber": 1,
    "currentTripDayId": 1,
    "isLastDay": false,
    "pledgeCompleted": true,
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "route": null,
        "stops": []
      }
    ]
  }
}
```

`pledgeCompleted=true`는 자녀와 참여 부모 최소 1명의 서명이 완료되었다는 뜻입니다.
상세 정책은 `docs/policy/travel-mode.md`를 따릅니다.

---

## POST /api/v1/trips/{tripId}/course/recommendation

여행을 만든 자녀가 여행 추천 코스를 생성합니다.
요청 본문은 받지 않습니다. 서버는 여행 생성 시 저장한 `recommendationSnapshot`, 여행 도시, 여행 일자를 기준으로 TourAPI 장소 후보를 조회합니다.
`planning`, `ready`, `in_progress` 상태에서 호출할 수 있고 `completed`, `archived` 상태에서는 호출할 수 없습니다.

생성 규칙:

- 숙박을 제외한 TourAPI 콘텐츠 타입을 후보로 사용합니다.
- 부모 프로필의 `walkingPace` 기준으로 하루 장소 수를 정합니다.
- 부모가 2명인 경우 더 천천히 걷는 부모 기준을 사용합니다.
- 음식점 후보가 있으면 각 일자에 식사 장소 1곳을 포함합니다.
- 이동 도움이 필요한 부모가 있으면 TourAPI 무장애 정보 문자열 존재 여부를 점수에 반영합니다.
- 기존 코스가 있으면 추천 결과로 덮어씁니다.
- 추천 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`가 됩니다.

추천 정책 상세는 `docs/policy/course-recommendation.md`를 따릅니다.

### Request

없음.

### Response

`GET /api/v1/trips/{tripId}/course`와 같은 `TripCourseResponse` 형태입니다.

```json
{
  "status": 200,
  "success": true,
  "message": "여행 추천 코스 생성 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    },
    "status": "ready",
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "stops": [
          {
            "id": 1,
            "sortOrder": 1,
            "stopType": "sightseeing",
            "sourceProvider": "tour_api",
            "externalPlaceId": "988449",
            "contentTypeId": "12",
            "name": "오도리 공원",
            "category": "관광지",
            "recommendationReason": "부모님 선호 테마와 무장애 정보를 함께 반영한 추천 장소입니다.",
            "recommendationTags": ["tour_api", "type:12", "na", "mobility_info"],
            "sourcePayload": {
              "provider": "tour_api",
              "recommendation": {
                "policyVersion": "tour-api-course-recommendation-v1",
                "score": 51
              }
            },
            "isManualAdded": false
          }
        ]
      }
    ]
  }
}
```

TourAPI 서비스키가 서버에 설정되어 있지 않거나 TourAPI 호출이 실패하면 `500`을 반환합니다.

---

## POST /api/v1/trips/{tripId}/days/{dayNumber}/route-optimization

여행을 만든 자녀가 특정 일자의 방문지 순서를 Tmap 기준으로 최적화합니다.
사용자가 시작점/도착점을 입력하지 않으므로 서버가 모든 시작/도착 조합을 탐색한 뒤 가장 짧은 결과를 선택합니다.
`planning`, `ready`, `in_progress` 상태에서 호출할 수 있고 `completed`, `archived` 상태에서는 호출할 수 없습니다.

최적화 결과는 아래에 반영됩니다.

- `trip_stops.sort_order`
- `trip_stops.arrival_time`
- 비어 있던 `trip_stops.dwell_minutes` 기본값
- `trip_days`의 일자별 route 캐시

제약:

- 하루 방문지 3곳 이상, 10곳 이하만 처리합니다.
- 모든 방문지에 `latitude`, `longitude`가 있어야 합니다.
- Tmap 앱키가 서버에 설정되어 있어야 합니다.

정책 상세는 `docs/policy/route-optimization.md`를 따릅니다.

### Request

없음.

### Response

`GET /api/v1/trips/{tripId}/course`와 같은 `TripCourseResponse` 형태입니다.

```json
{
  "status": 200,
  "success": true,
  "message": "여행 일자 경로 최적화 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    },
    "status": "ready",
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "route": {
          "provider": "tmap",
          "totalDistanceMeters": 6230,
          "totalDurationSeconds": 1757,
          "optimizedAt": "2026-07-07T12:00:00",
          "polyline": [
            {
              "longitude": 129.2247,
              "latitude": 35.8562
            }
          ],
          "sourcePayload": {
            "provider": "tmap",
            "operation": "routeOptimization10",
            "policyVersion": "tmap-route-optimization-v1",
            "orderedStopIds": [2, 3, 1]
          }
        },
        "stops": [
          {
            "id": 2,
            "sortOrder": 1,
            "name": "경주 한식당",
            "arrivalTime": "10:00:00",
            "dwellMinutes": 60
          }
        ]
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}/places/search

코스 편집 화면에서 여행 도시 범위 안의 TourAPI 방문지를 키워드로 검색합니다.
검색 결과는 추천 후보 목록으로만 사용하며, 사용자가 선택한 장소를 코스에 반영하려면 `PUT /api/v1/trips/{tripId}/course`로 저장해야 합니다.

숙박은 검색 결과에서 제외합니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `keyword` | string | true | 검색어. 공백 제거 후 1자 이상, 50자 이하 |
| `contentTypeId` | string | false | TourAPI 콘텐츠 타입 필터. 생략하면 숙박을 제외한 지원 타입 전체 |
| `page` | number | false | 페이지 번호. 기본값 `1` |
| `size` | number | false | 페이지 크기. 기본값 `20`, 최대 `50` |

지원 `contentTypeId`:

| contentTypeId | 의미 | 저장 시 기본 stopType |
|---------------|------|----------------------|
| `12` | 관광지 | `sightseeing` |
| `14` | 문화시설 | `sightseeing` |
| `15` | 행사/공연/축제 | `sightseeing` |
| `28` | 레포츠 | `sightseeing` |
| `38` | 쇼핑 | `sightseeing` |
| `39` | 음식점 | `meal` |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "방문지 검색 성공",
  "data": {
    "tripId": 1,
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 3,
      "badgeLabel": "인기"
    },
    "keyword": "경주 맛집",
    "contentTypeId": "39",
    "page": 1,
    "size": 20,
    "places": [
      {
        "sourceProvider": "tour_api",
        "externalPlaceId": "988449",
        "contentTypeId": "39",
        "contentTypeName": "음식점",
        "stopType": "meal",
        "name": "경주 한식당",
        "category": "음식점",
        "address": "경상북도 경주시",
        "latitude": 35.8562,
        "longitude": 129.2247,
        "phone": "054-000-0000",
        "imageUrl": "https://example.com/place.jpg",
        "lclsSystm1": "FD"
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}/places/{contentId}

코스 편집 화면에서 TourAPI 방문지 상세와 무장애 주요 정보를 조회합니다.
장소 상세 화면 표시와, 선택 장소를 코스 저장 API에 넣기 전 스냅샷 데이터를 구성하는 데 사용합니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `contentTypeId` | string | false | 검색 목록에서 알고 있는 TourAPI 콘텐츠 타입 ID. 상세 응답에 타입이 없을 때 보조값으로 사용 |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "방문지 상세 조회 성공",
  "data": {
    "sourceProvider": "tour_api",
    "externalPlaceId": "988449",
    "contentTypeId": "12",
    "contentTypeName": "관광지",
    "stopType": "sightseeing",
    "name": "오도리 공원",
    "category": "관광지",
    "address": "경상북도 경주시",
    "latitude": 35.8562,
    "longitude": 129.2247,
    "phone": "054-000-0000",
    "homepageUrl": "https://example.com",
    "imageUrl": "https://example.com/place.jpg",
    "overview": "산책하기 좋은 공원입니다.",
    "lclsSystm1": "NA",
    "accessibility": {
      "parking": "장애인 주차장 있음",
      "publicTransport": null,
      "route": "출입구까지 경사로 있음",
      "wheelchair": null,
      "exit": "휠체어 접근 가능",
      "elevator": null,
      "restroom": "장애인 화장실 있음"
    },
    "recommendationTags": ["tour_api", "type:12", "na", "mobility_info"],
    "sourcePayload": {
      "provider": "tour_api",
      "detailCommon": {
        "contentid": "988449"
      },
      "accessibility": {
        "route": "출입구까지 경사로 있음"
      }
    }
  }
}
```

---

## PUT /api/v1/trips/{tripId}/course

여행을 만든 자녀가 일자별 방문지 코스를 전체 저장합니다.
요청 배열 순서가 해당 일자의 방문 순서가 됩니다.
기존 코스는 저장 요청 기준으로 덮어쓰며, 요청에 포함하지 않은 일자는 빈 코스로 저장됩니다.
코스를 저장하면 기존 Tmap 경로 최적화 캐시는 무효화됩니다.
`planning`, `ready`, `in_progress` 상태에서 저장할 수 있고 `completed`, `archived` 상태에서는 저장할 수 없습니다.

> 이 엔드포인트가 방문지 추가·수정·삭제·순서 변경을 모두 처리합니다. 단건 편집 API가 아닌 전체 덮어쓰기이므로 클라이언트는 화면에서 임시 편집한 뒤, 변경하지 않은 일자를 포함한 최종 코스 전체를 `저장하기` 시점에 전송해야 합니다.

### Request

```json
{
  "days": [
    {
      "dayNumber": 1,
      "stops": [
        {
          "stopType": "sightseeing",
          "sourceProvider": "tour_api",
          "externalPlaceId": "988449",
          "contentTypeId": "12",
          "name": "오도리 공원",
          "category": "관광지",
          "address": "대구광역시 동구 효목동",
          "latitude": 35.8821234,
          "longitude": 128.6212345,
          "arrivalTime": "10:30",
          "dwellMinutes": 60,
          "note": "부모님과 사진 찍기",
          "recommendationReason": "짧은 산책과 휴식에 적합합니다.",
          "recommendationTags": ["nature_scenery", "low_slope"],
          "sourcePayload": {
            "contentid": "988449",
            "route": "출입구까지 경사로가 설치되어 있음"
          },
          "isManualAdded": false
        }
      ]
    }
  ]
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `days[].dayNumber` | number | true | 여행 며칠차. 해당 여행에 존재하는 일자만 가능 |
| `days[].stops[]` | array | false | 해당 일자의 방문지 목록. 배열 순서대로 `sortOrder` 저장 |
| `stops[].stopType` | enum | false | `sightseeing`, `meal`, `rest`, `cafe`. 기본값 `sightseeing` |
| `stops[].sourceProvider` | enum | false | `tour_api`, `tmap`, `kakao_map`, `public_data`, `local_excel`, `internal`. 기본값 `tour_api` |
| `stops[].externalPlaceId` | string | false | 외부 장소 ID. TourAPI contentId, 지도 API 장소 ID 등 |
| `stops[].contentTypeId` | string | false | TourAPI contentTypeId |
| `stops[].name` | string | true | 장소명 |
| `stops[].category` | string | false | 장소 카테고리 |
| `stops[].address` | string | false | 주소 |
| `stops[].latitude` | decimal | false | 위도 |
| `stops[].longitude` | decimal | false | 경도 |
| `stops[].phone` | string | false | 전화번호 |
| `stops[].homepageUrl` | string | false | 홈페이지 URL |
| `stops[].imageUrl` | string | false | 대표 이미지 URL |
| `stops[].overview` | string | false | 장소 소개 |
| `stops[].arrivalTime` | time | false | 도착 예정 시간 |
| `stops[].dwellMinutes` | number | false | 예상 체류 시간(분) |
| `stops[].note` | string | false | 방문지 메모 |
| `stops[].recommendationReason` | string | false | 추천 이유 |
| `stops[].recommendationTags` | string array | false | 추천 태그 |
| `stops[].sourcePayload` | object | false | 외부 API 원본 응답 일부. 후속 API 연동 전까지 유동 필드 보관용 |
| `stops[].isManualAdded` | boolean | false | 사용자가 직접 추가한 장소 여부 |

### Response

`GET /api/v1/trips/{tripId}/course`와 같은 `TripCourseResponse` 형태입니다.

같은 가족의 부모는 코스를 조회할 수 있지만 저장, 추천 재생성, 경로 최적화는 할 수 없습니다.
방문지 편집 흐름과 전체 덮어쓰기 기준은 `docs/policy/trip-edit.md`를 따릅니다.

---

## GET /api/v1/trips/{tripId}/nearby-restrooms

여행 기간 중 같은 가족 구성원이 현재 위치 주변 공중화장실을 조회합니다.
클라이언트가 반경이나 개수를 지정하지 않으며, 서버가 직선거리 5km 이내에서 가까운 순으로 최대 10개를 반환합니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `latitude` | number | true | 현재 위치 WGS84 위도. `-90` 이상 `90` 이하 |
| `longitude` | number | true | 현재 위치 WGS84 경도. `-180` 이상 `180` 이하 |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "주변 공중화장실 조회 성공",
  "data": [
    {
      "id": 1,
      "name": "사직단 공중화장실",
      "address": "서울특별시 종로구 사직동 1-28",
      "latitude": 37.5758692,
      "longitude": 126.9684817,
      "distanceMeters": 320,
      "phone": "02-2148-2832",
      "operatingHours": "상시"
    }
  ]
}
```

5km 이내 화장실이 없으면 `data`는 빈 배열입니다.
여행 시작 전과 종료 후에는 HTTP `200`/본문 `status=400`, 다른 가족 사용자에게는 HTTP `200`/본문 `status=403`을 반환합니다.

---

## GET /api/v1/trips/{tripId}/nearby-hospitals

여행 기간 중 같은 가족 구성원이 현재 위치 기준 5km 이내 병원을 가까운 순으로 최대 10개 조회합니다.
서버는 Tmap POI 주변 카테고리 검색의 `병원` 카테고리를 실시간으로 조회하며 결과를 DB에 저장하지 않습니다.
Tmap 분류상 의원·치과 등이 포함될 수 있고 응급실 운영 여부는 보장하지 않습니다.

Query Parameters와 접근 정책은 주변 공중화장실 조회 API와 같습니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "주변 병원 조회 성공",
  "data": [
    {
      "id": "12345678",
      "type": "hospital",
      "name": "서울대학교병원",
      "address": "서울 종로구 대학로 101",
      "latitude": 37.579617,
      "longitude": 126.998998,
      "distanceMeters": 320,
      "phone": "02-2072-2114"
    }
  ]
}
```

Tmap 호출·응답 처리 실패는 실제 HTTP `500`, 본문 `status=500`을 반환합니다.

---

## GET /api/v1/trips/{tripId}/nearby-pharmacies

여행 기간 중 같은 가족 구성원이 현재 위치 기준 5km 이내 약국을 가까운 순으로 최대 10개 조회합니다.
서버는 Tmap POI 주변 카테고리 검색의 `약국` 카테고리를 실시간으로 조회하며 결과를 DB에 저장하지 않습니다.

Query Parameters, 접근 정책, 응답 필드는 주변 병원 조회 API와 같고 `type`은 `pharmacy`, 성공 메시지는 `주변 약국 조회 성공`입니다.
