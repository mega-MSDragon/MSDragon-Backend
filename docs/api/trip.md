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
- 실제 공공데이터/Tmap 연동과 추천 코스 생성은 후속 작업입니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/trips/parent-candidates` | 여행 대상 부모 후보 조회 |
| `GET` | `/api/v1/trips/destinations` | 여행 도시 목록 조회 |
| `GET` | `/api/v1/trips` | 내 가족 여행 목록 조회 |
| `GET` | `/api/v1/trips/{tripId}` | 여행 상세 조회 |
| `POST` | `/api/v1/trips` | 여행 생성 |

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

생성 직후 `status`는 `planning`입니다. 코스 생성/편집 기능이 붙으면 `ready`, `in_progress`, `completed` 전환을 별도 정책으로 확정합니다.
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
