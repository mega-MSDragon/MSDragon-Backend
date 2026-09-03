# Home API

홈 화면의 나의 여행, 월별 추천 여행, 축제 섹션을 각각 조회합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다. 인증 오류처럼 서버가 처리한 실패는 실제 HTTP `200`으로 반환하고 본문의 `status`와 `success`로 구분합니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/home/my-trips` | 나의 여행과 부모별 프로필 상태 조회 |
| `GET` | `/api/v1/home/monthly-recommendations` | 월별 추천 도시 조회 |
| `GET` | `/api/v1/home/sections` | 홈 축제 영역부터 아래까지의 동적 섹션 조회 |
| `GET` | `/api/v1/home/festivals` | 진행·예정 축제 조회. **deprecated** — `sections`의 `festival_collection`으로 대체 |

## GET /api/v1/home/my-trips

요청 본문과 쿼리 파라미터가 없습니다.

```json
{
  "status": 200,
  "success": true,
  "message": "나의 여행 조회 성공",
  "data": {
    "familyId": 1,
    "userRole": "child",
    "parentProfiles": [
      {
        "userId": 2,
        "displayName": "김영희",
        "relationLabel": "엄마",
        "profileCompleted": true
      },
      {
        "userId": 3,
        "displayName": "김철수",
        "relationLabel": "아빠",
        "profileCompleted": false
      }
    ],
    "trips": [
      {
        "id": 1,
        "title": "경주 가족 여행",
        "destination": {
          "code": "gyeongju",
          "displayName": "경주",
          "displayOrder": 2,
          "badgeLabel": null
        },
        "startDate": "2026-05-10",
        "endDate": "2026-05-11",
		"dayTrip": false,
        "status": "in_progress",
        "dDay": null,
        "primaryTheme": "history_culture",
		"intensity": "low",
		"ratings": [
		  {
			"parentUserId": 2,
			"displayName": "김영희",
			"relationLabel": "엄마",
			"overallRating": 4.5
		  }
		]
      }
    ]
  }
}
```

| Field | Type | Nullable | 설명 |
|-------|------|----------|------|
| `familyId` | number | true | 가족 매칭 전이면 `null` |
| `userRole` | enum | false | `child`, `parent` |
| `parentProfiles` | array | false | 자녀는 연결된 부모 전체, 부모는 본인 한 건 |
| `parentProfiles[].profileCompleted` | boolean | false | 해당 부모의 상세 프로필 완성 여부 |
| `trips` | array | false | `in_progress`, `planning`, `ready` 여행 포함. `completed`는 **자녀에게 노출하지 않고**, 부모에게는 **아직 평가하지 않은 여행만** 노출합니다 |
| `trips[].dayTrip` | boolean | false | 시작일과 종료일이 같은 당일치기 여부. `true`이면 날짜를 한 번만 표시 |
| `trips[].dDay` | number | true | 시작일까지 남은 일수. 진행 중이면 `null` |
| `trips[].primaryTheme` | enum | true | 여행 생성 당시 부모 프로필 스냅샷에서 계산한 대표 테마 |
| `trips[].intensity` | enum | true | `low`, `normal`, `high`. 스냅샷이 없으면 `null` |
| `trips[].ratings` | array | false | 제출된 부모별 이름, 관계명, 별점. 아직 제출된 피드백이 없으면 빈 배열 |

자녀는 완료된 여행을 기록 탭(`GET /api/v1/records`)에서 확인합니다. 부모 홈에 남은 완료 여행은 평가 유도용이며 본인 피드백을 제출하면 사라집니다. 자세한 기준은 `docs/policy/home.md`를 따릅니다.

클라이언트는 `userRole=child`일 때만 새 여행 생성 기능을 노출합니다. `parentProfiles[].profileCompleted=false`인 부모가 있으면 해당 부모의 프로필 작성 또는 요청 UI를 표시합니다.

## GET /api/v1/home/monthly-recommendations

요청 본문과 쿼리 파라미터가 없습니다.

```json
{
  "status": 200,
  "success": true,
  "message": "월별 추천 여행 조회 성공",
  "data": {
    "recommendationMonth": 5,
    "recommendedCities": [
      {
        "code": "gyeongju",
        "displayName": "경주",
        "imageUrl": "https://api.ms-dragon.com/images/destinations/gyeongju.png"
      }
    ]
  }
}
```

- `recommendationMonth`: 추천 정책에 사용한 서울 기준 월입니다.
- `recommendedCities`: 월별 고정 추천 도시 5개입니다.
- `imageUrl`은 서버에 넣어둔 도시 이미지 URL입니다(`{BASE_URL}/images/destinations/{code}.png`). 인증 없이 접근할 수 있습니다.
- `imageUrl=null`: 서버 이미지가 없고 TourAPI 폴백 조회까지 실패한 경우이며 앱의 도시별 기본 이미지를 사용합니다.

## GET /api/v1/home/sections

홈 축제 영역부터 아래까지를 섹션 목록으로 조회합니다. 섹션을 추가하거나 순서를 바꿀 때 클라이언트를 고치지 않도록 서버가 구성과 순서를 결정합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "홈 섹션 조회 성공",
  "data": {
    "sections": [
      {
        "key": "festivals",
        "type": "festival_collection",
        "title": "지금 열리는 축제",
        "subtitle": "오늘부터 30일 안에 만나요",
        "items": [
          {
            "contentId": "250119",
            "title": "안동 선유줄불놀이",
            "summary": "낙동강 위로 불꽃이 이어지는 전통 축제입니다.",
            "imageUrl": "https://example.com/festival.jpg",
            "address": "경상북도 안동시 풍천면",
            "regionName": "안동",
            "tags": ["안동", "축제"],
            "eventStartDate": "2026-08-01",
            "eventEndDate": "2026-08-31"
          }
        ]
      },
      {
        "key": "monthly_attractions",
        "type": "attraction_collection",
        "title": "이번 달 가볼 만한 곳",
        "subtitle": "강릉·속초 · 경주 · 대구 · 서울 · 제주",
        "items": [
          {
            "contentId": "126508",
            "title": "불국사",
            "summary": null,
            "imageUrl": "https://example.com/bulguksa.jpg",
            "address": "경상북도 경주시 진현동",
            "regionName": "경주",
            "tags": ["경주", "관광지"],
            "eventStartDate": null,
            "eventEndDate": null
          }
        ]
      }
    ]
  }
}
```

### 클라이언트 구현 규칙

두 규칙을 지켜야 서버가 섹션을 바꿀 때 앱을 고치지 않아도 됩니다.

1. **`sections` 배열 순서대로 렌더링합니다.** 노출 순서는 서버가 결정하므로 클라이언트가 재정렬하지 않습니다.
2. **모르는 `type`의 섹션은 건너뜁니다.** 이 규칙이 없으면 서버가 섹션을 추가할 때마다 앱 강제 업데이트가 필요해집니다.

`items`가 빈 배열인 섹션은 외부 API 장애로 항목을 채우지 못한 경우입니다. 섹션 전체를 숨기거나 빈 상태로 표시하는 것은 클라이언트가 결정합니다.

### 필드

| Field | Type | Nullable | 설명 |
|-------|------|----------|------|
| `sections[].key` | string | false | 섹션 식별자. 같은 `type`을 여러 섹션에서 쓸 수 있어 화면 상태 관리에 사용합니다 |
| `sections[].type` | enum | false | `festival_collection`, `attraction_collection`. 렌더러 선택에 사용합니다 |
| `sections[].title` | string | false | 섹션 제목 |
| `sections[].subtitle` | string | true | 섹션 부제 |
| `sections[].items[]` | array | false | 섹션 항목. 모든 섹션이 같은 형태를 사용합니다 |
| `items[].eventStartDate` | date | true | `festival_collection`만 값이 있습니다 |
| `items[].eventEndDate` | date | true | `festival_collection`만 값이 있습니다 |

`items`는 섹션 종류와 무관하게 같은 형태입니다. 타입별로 다른 JSON을 주면 클라이언트 디코딩이 복잡해지므로, 해당 섹션에 없는 필드는 `null`로 내립니다.

### 섹션 종류

| `type` | 내용 | 출처 |
|--------|------|------|
| `festival_collection` | 오늘부터 30일 이내 축제 최대 10개 | TourAPI `searchFestival2` |
| `attraction_collection` | 이번 달 추천 도시 5곳의 관광지, 도시당 2개 | TourAPI `areaBasedList2` (`contentTypeId=12`) |

구성과 순서는 `HomeSectionPolicy`가 정하며 정책은 `docs/policy/home.md`를 따릅니다.

---

## GET /api/v1/home/festivals

> **Deprecated.** `GET /api/v1/home/sections`의 `festival_collection` 섹션으로 대체되었습니다.
> 심사 중이거나 이미 배포된 앱 버전이 이 API를 호출하고 있어 **제거하지 않고 유지**합니다.
> 해당 버전이 사용되지 않게 되면 제거합니다. 내부적으로 `sections`와 같은 축제 데이터·캐시를 공유합니다.

요청 본문과 쿼리 파라미터가 없습니다.

```json
{
  "status": 200,
  "success": true,
  "message": "축제 조회 성공",
  "data": {
    "festivals": [
      {
        "contentId": "250119",
        "title": "안동 선유줄불놀이",
        "summary": "낙동강 위로 불꽃이 이어지는 전통 축제입니다.",
        "imageUrl": "https://example.com/festival.jpg",
        "address": "경상북도 안동시 풍천면",
        "regionName": "안동",
        "eventStartDate": "2026-05-10",
        "eventEndDate": "2026-05-31",
        "tags": ["안동", "축제"]
      }
    ]
  }
}
```

`festivals`는 진행 중이거나 30일 이내 시작하는 축제를 최대 10개 반환합니다. TourAPI 장애 시 직전 캐시 또는 빈 목록을 반환하며 다른 홈 섹션에는 영향을 주지 않습니다.

`부모님께 부탁드리기`의 실제 푸시 전송은 이번 API 범위에 포함하지 않습니다. `나중에 하기` 선택도 서버에 저장하지 않고 앱에서 처리합니다.
