# Home API

홈 화면에 필요한 사용자 역할, 부모님 프로필 안내, 진행·예정 여행, 월별 추천 도시와 축제를 한 번에 조회합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 인증 오류처럼 서버가 처리한 실패는 실제 HTTP `200`으로 반환하고 본문의 `status`와 `success`로 구분합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/home` | 홈 화면 집계 조회 |

---

## GET /api/v1/home

### Request

요청 본문과 쿼리 파라미터가 없습니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "홈 조회 성공",
  "data": {
    "familyId": 1,
    "userRole": "child",
    "canCreateTrip": true,
    "profileGuide": {
      "type": "request_parent_profile",
      "targets": [
        {
          "userId": 3,
          "displayName": "김철수",
          "relationLabel": "아빠"
        }
      ]
    },
    "trips": [
      {
        "id": 1,
        "title": "경주 가족 여행",
        "destination": {
          "code": "gyeongju",
          "displayName": "경주",
          "displayOrder": 3,
          "badgeLabel": "인기"
        },
        "startDate": "2026-05-10",
        "endDate": "2026-05-11",
        "status": "in_progress",
        "dDay": null,
        "primaryTheme": "history_culture",
        "intensity": "low"
      },
      {
        "id": 2,
        "title": "우리 가족 힐링 여행",
        "destination": {
          "code": "gangneung_sokcho",
          "displayName": "강릉·속초",
          "displayOrder": 2,
          "badgeLabel": null
        },
        "startDate": "2026-06-27",
        "endDate": "2026-06-29",
        "status": "ready",
        "dDay": 48,
        "primaryTheme": "nature_scenery",
        "intensity": "high"
      }
    ],
    "recommendationMonth": 5,
    "recommendedCities": [
      {
        "code": "gangneung_sokcho",
        "displayName": "강릉·속초",
        "imageUrl": "https://example.com/gangneung.jpg"
      },
      {
        "code": "gyeongju",
        "displayName": "경주",
        "imageUrl": "https://example.com/gyeongju.jpg"
      },
      {
        "code": "busan",
        "displayName": "부산",
        "imageUrl": "https://example.com/busan.jpg"
      }
    ],
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

### 필드 설명

| Field | Type | Nullable | 설명 |
|-------|------|----------|------|
| `familyId` | number | true | 가족 매칭 전이면 `null` |
| `userRole` | enum | false | `child`, `parent` |
| `canCreateTrip` | boolean | false | 현재 자녀만 `true` |
| `profileGuide` | object | true | 프로필 안내가 필요 없으면 `null` |
| `profileGuide.type` | enum | false | `complete_my_profile`, `request_parent_profile` |
| `profileGuide.targets` | array | false | 프로필 작성이 필요한 부모 목록 |
| `trips` | array | false | `in_progress`, `planning`, `ready` 여행만 포함 |
| `trips[].dDay` | number | true | 시작일까지 남은 일수. 진행 중이면 `null` |
| `trips[].primaryTheme` | enum | true | 여행 생성 당시 부모 프로필 스냅샷에서 계산한 대표 테마 |
| `trips[].intensity` | enum | true | `low`, `normal`, `high`. 스냅샷이 없으면 `null` |
| `recommendationMonth` | number | false | 추천 도시 정책에 사용한 서울 기준 월 |
| `recommendedCities` | array | false | 월별 고정 추천 도시 3개 |
| `recommendedCities[].imageUrl` | string | true | TourAPI 이미지 조회 실패 시 `null` |
| `festivals` | array | false | 진행 중이거나 30일 이내 시작하는 축제. 최대 10개 |

### 클라이언트 화면 분기

- `trips=[]`이고 `canCreateTrip=true`: 자녀용 여행 없음 화면과 새 여행 추가 버튼을 표시합니다.
- `trips=[]`이고 `canCreateTrip=false`: 부모용 여행 없음 화면을 표시하고 새 여행 추가 버튼은 숨깁니다.
- `profileGuide.type=complete_my_profile`: 부모 본인의 `내 취향 알려주기` 화면을 표시합니다.
- `profileGuide.type=request_parent_profile`: 자녀가 새 여행 추가를 누를 때 부모님 프로필 요청 팝업을 표시할 수 있습니다.
- `profileGuide=null`: 프로필 관련 안내가 필요하지 않습니다.
- `recommendedCities[].imageUrl=null`: 앱에 포함된 도시 코드별 기본 이미지를 표시합니다.
- `festivals=[]`: 축제 영역을 숨기거나 빈 상태로 표시합니다. 홈 전체를 오류 처리하지 않습니다.

`부모님께 부탁드리기`의 실제 푸시 전송은 이번 API 범위에 포함하지 않습니다. `나중에 하기` 선택도 서버에 저장하지 않고 앱에서 처리합니다.
