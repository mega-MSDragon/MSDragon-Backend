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
  "activityLevel": "moderate",
  "needsMobilityAssistance": true,
  "themeCodes": ["nature", "history"],
  "foodPreference": "korean_only",
  "avoidSpicy": true,
  "complete": true
}
```

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `currentStep` | number | false | `1`~`3` |
| `activityLevel` | enum | false | `slow`, `moderate`, `active` |
| `needsMobilityAssistance` | boolean | false | - |
| `themeCodes` | enum array | false | `nature`, `history`, `activity`, `food`, `culture`, `landmark` |
| `foodPreference` | enum | false | `korean_only`, `familiar_food`, `open_minded` |
| `avoidSpicy` | boolean | false | - |
| `complete` | boolean | false | 기본값 `false` |

`themeCodes`는 최대 3개까지 저장할 수 있습니다.
`complete=true`이면 `activityLevel`, `needsMobilityAssistance`, `foodPreference`가 필요하며 추천용 여행 MBTI를 계산합니다.

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
    "activityLevel": "moderate",
    "needsMobilityAssistance": true,
    "themeCodes": ["nature", "history"],
    "foodPreference": "korean_only",
    "avoidSpicy": true,
    "personalityType": "history_walker",
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

현재 구현은 부모님 프로필 완료 시 아래 enum 중 하나를 `personalityType`으로 저장합니다.

| Value | 설명 |
|-------|------|
| `city_taster` | 도시형 탐험가 |
| `sensitive_culture` | 감성 문화러형 |
| `relaxed_explorer` | 유유자적 힐링형 |
| `history_walker` | 역사 산책가형 |
| `active_experiencer` | 액티비티 열정형 |
| `local_challenger` | 로컬 도전가형 |
