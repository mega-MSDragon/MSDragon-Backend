# Home API

홈 화면의 나의 여행, 월별 추천 여행, 축제 섹션을 각각 조회합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다. 인증 오류처럼 서버가 처리한 실패는 실제 HTTP `200`으로 반환하고 본문의 `status`와 `success`로 구분합니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/home/my-trips` | 나의 여행과 부모별 프로필 상태 조회 |
| `GET` | `/api/v1/home/monthly-recommendations` | 월별 추천 도시 조회 |
| `GET` | `/api/v1/home/festivals` | 진행·예정 축제 조회 |

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
| `trips` | array | false | `in_progress`, `completed`, `planning`, `ready` 여행 포함 |
| `trips[].dayTrip` | boolean | false | 시작일과 종료일이 같은 당일치기 여부. `true`이면 날짜를 한 번만 표시 |
| `trips[].dDay` | number | true | 시작일까지 남은 일수. 진행 중이면 `null` |
| `trips[].primaryTheme` | enum | true | 여행 생성 당시 부모 프로필 스냅샷에서 계산한 대표 테마 |
| `trips[].intensity` | enum | true | `low`, `normal`, `high`. 스냅샷이 없으면 `null` |
| `trips[].ratings` | array | false | 제출된 부모별 이름, 관계명, 별점. 아직 제출된 피드백이 없으면 빈 배열 |

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

## GET /api/v1/home/festivals

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
