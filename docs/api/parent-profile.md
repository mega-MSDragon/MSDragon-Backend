# Parent Profile API

부모님 상세 프로필 작성/조회 API입니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

---

## 권한 정책

- 부모는 본인 부모님 프로필만 작성/수정할 수 있습니다.
- 부모는 본인 부모님 프로필을 조회할 수 있습니다.
- 자녀는 같은 가족으로 연결된 부모님 프로필만 조회할 수 있습니다.
- 자녀는 부모님 프로필을 작성하거나 수정할 수 없습니다.
- 아직 부모님 프로필이 저장되지 않은 경우 조회 API는 빈 `draft` 응답을 반환합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/parent-profiles/me` | 부모 본인 프로필 조회 |
| `PUT` | `/api/v1/parent-profiles/me` | 부모 본인 프로필 작성/수정 |
| `GET` | `/api/v1/parent-profiles/{parentUserId}` | 연결된 부모 프로필 조회 |

---

## PUT /api/v1/parent-profiles/me

부모 사용자가 본인의 상세 프로필을 단계별로 저장하거나 완료 처리합니다.

### Request

```json
{
  "currentStep": 3,
  "walkingPace": "normal",
  "needsMobilityAssistance": true,
  "travelThemes": ["nature_scenery", "history_culture"],
  "foodPreference": "korean",
  "complete": true
}
```

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `currentStep` | number | false | `1`~`3` |
| `walkingPace` | enum | false | `slow`, `normal`, `fast` |
| `needsMobilityAssistance` | boolean | false | 완료 시 `true`/`false` 필수 |
| `travelThemes` | enum array | false | `nature_scenery`, `history_culture`, `shopping`, `activity`, `culture_life`, `landmark`, `experience` |
| `foodPreference` | enum | false | `korean`, `familiar`, `adventurous` |
| `complete` | boolean | false | 기본값 `false` |

`travelThemes`는 최소 1개, 최대 3개까지 저장할 수 있습니다.
`complete=true`이면 `walkingPace`, `needsMobilityAssistance`, `travelThemes`, `foodPreference`가 모두 필요하며 추천용 여행 MBTI를 계산합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "내 부모님 프로필 저장 성공",
  "data": {
    "id": 1,
    "parentUserId": 2,
    "profileExists": true,
    "status": "completed",
    "currentStep": 3,
    "walkingPace": "normal",
    "needsMobilityAssistance": true,
    "travelThemes": ["nature_scenery", "history_culture"],
    "foodPreference": "korean",
    "personalityType": "heritage_walker",
    "completionPercent": 100,
    "completedAt": "2026-07-01T12:00:00"
  }
}
```

---

## GET /api/v1/parent-profiles/me

부모 사용자가 본인의 상세 프로필을 조회합니다.

자녀가 호출하면 `400 Bad Request`를 반환합니다.

### Response

`PUT /api/v1/parent-profiles/me`와 같은 응답 형태입니다.

---

## GET /api/v1/parent-profiles/{parentUserId}

같은 가족으로 연결된 자녀가 부모님의 상세 프로필을 조회합니다.
부모 본인도 같은 경로로 본인 프로필을 조회할 수 있습니다.

연결되지 않은 사용자가 조회하면 `403 Forbidden`을 반환합니다.

### Response

`PUT /api/v1/parent-profiles/me`와 같은 응답 형태입니다.

---

## 추천용 여행 MBTI

현재 구현은 부모님 프로필 완료 시 `docs/policy/parent-travel-mbti.md`의 가중치 정책으로 아래 enum 중 하나를 `personalityType`에 저장합니다.

| Value | 설명 |
|-------|------|
| `urban_explorer` | 도시 취향 탐험가 |
| `culture_stroller` | 감성 문화 산책가 |
| `healing_traveler` | 유유자적 힐링러 |
| `heritage_walker` | 역사 산책가 |
| `active_adventurer` | 액티비티 열정가 |
| `local_challenger` | 로컬 도전가형 |
