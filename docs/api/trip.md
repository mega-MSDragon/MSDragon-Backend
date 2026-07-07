# Trip API

여행 생성 기본 흐름을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

---

## 결정 사항

- 여행 생성은 현재 자녀 사용자만 가능합니다.
- 여행 대상은 같은 가족에 연결된 부모만 선택할 수 있습니다.
- 선택한 부모의 상세 프로필이 `completed` 상태여야 여행을 생성할 수 있습니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 같은 가족에서 날짜가 겹치는 여행은 생성할 수 없습니다.
- 도시 목록은 현재 서버 고정 catalog로 내려주고, 여행에는 `destinationCode` 문자열을 저장합니다.
- 여행 생성 시 선택한 부모의 추천 입력값을 `recommendationSnapshot`으로 저장합니다. 이후 부모 프로필이 수정되어도 생성 당시 추천 기준은 유지됩니다.
- 여행 코스는 일자별 방문지 목록을 전체 저장합니다. 요청 배열 순서가 방문 순서가 되며, 포함하지 않은 일자는 빈 코스로 저장됩니다.
- 저장된 방문지는 외부 API 원본이 바뀌어도 기존 코스를 유지할 수 있도록 `trip_stops`에 장소 스냅샷을 저장합니다.
- 추천 코스 생성은 한국관광공사 TourAPI 무장애여행 서비스로 장소 후보를 조회하고, 부모 프로필 스냅샷으로 점수를 계산해 `trip_stops`에 저장합니다.
- 추천 코스 생성은 장소 추천까지만 수행합니다. Tmap 경로/거리/소요시간 계산은 후속 작업입니다.
- 추천 코스 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`로 변경됩니다.
- 코스 편집 중 방문지 검색/상세 조회는 TourAPI를 조회해 후보를 내려주며, 실제 코스 반영은 클라이언트가 선택한 장소를 `PUT /api/v1/trips/{tripId}/course`로 전체 저장할 때 이루어집니다.
- 방문지 검색/상세 조회에서도 숙박은 제외하고, TourAPI 원본 응답 일부는 코스 저장 시 `sourcePayload`로 보관할 수 있게 내려줍니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/trips/parent-candidates` | 여행 대상 부모 후보 조회 |
| `GET` | `/api/v1/trips/destinations` | 여행 도시 목록 조회 |
| `GET` | `/api/v1/trips` | 내 가족 여행 목록 조회 |
| `GET` | `/api/v1/trips/{tripId}` | 여행 상세 조회 |
| `GET` | `/api/v1/trips/{tripId}/course` | 여행 코스 조회 |
| `GET` | `/api/v1/trips/{tripId}/places/search` | 코스 편집용 방문지 검색 |
| `GET` | `/api/v1/trips/{tripId}/places/{contentId}` | 코스 편집용 방문지 상세 조회 |
| `POST` | `/api/v1/trips` | 여행 생성 |
| `POST` | `/api/v1/trips/{tripId}/course/recommendation` | 여행 추천 코스 생성 |
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

생성 직후 `status`는 `planning`입니다. 추천 코스 생성이 완료되면 `ready`가 되며, `in_progress`, `completed` 전환은 여행 모드 작업에서 확정합니다.
`recommendationSnapshot.policyVersion`은 부모 여행 MBTI 정책 버전을 의미합니다.

---

## GET /api/v1/trips

로그인 사용자가 속한 가족의 여행 목록을 조회합니다.
가족 매칭 전이면 빈 목록을 반환합니다.

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
다른 가족 여행이면 `403 Forbidden`을 반환합니다.

### Response

`POST /api/v1/trips`와 같은 `TripDetailResponse` 형태입니다.

---

## GET /api/v1/trips/{tripId}/course

같은 가족 구성원이 여행 코스를 조회합니다.
저장된 방문지가 없으면 여행 일자별로 `stops=[]`를 반환합니다.

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

## POST /api/v1/trips/{tripId}/course/recommendation

같은 가족 구성원이 여행 추천 코스를 생성합니다.
요청 본문은 받지 않습니다. 서버는 여행 생성 시 저장한 `recommendationSnapshot`, 여행 도시, 여행 일자를 기준으로 TourAPI 장소 후보를 조회합니다.

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

같은 가족 구성원이 일자별 방문지 코스를 전체 저장합니다.
요청 배열 순서가 해당 일자의 방문 순서가 됩니다.
기존 코스는 저장 요청 기준으로 덮어쓰며, 요청에 포함하지 않은 일자는 빈 코스로 저장됩니다.

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

현재 코스 저장은 같은 가족 구성원이면 가능합니다. 역할별 편집 제한이 필요해지면 별도 정책으로 좁힙니다.
