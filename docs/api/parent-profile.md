# Parent Profile API

부모님 상세 프로필 작성/조회 API입니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증·정책 오류도 HTTP `200`으로 반환하며, 본문의 `status`와 `success`로 구분합니다.

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
    "parentDisplayName": "영희",
    "profileExists": true,
    "status": "completed",
    "currentStep": 3,
    "walkingPace": "normal",
    "needsMobilityAssistance": true,
    "travelThemes": ["nature_scenery", "history_culture"],
    "foodPreference": "korean",
    "personalityType": "heritage_walker",
    "personalityResult": {
      "code": "heritage_walker",
      "name": "시간 여행자",
      "catchphrase": "아는 만큼 보인다!",
      "description": "유적지와 박물관을 산책하듯 둘러보며 지역의 이야기를 알아가는 걸 좋아해요.",
      "themes": ["history_culture", "nature_scenery", "landmark"]
    },
    "completionPercent": 100,
    "completedAt": "2026-07-01T12:00:00"
  }
}
```

결과 화면 제목은 `parentDisplayName`으로 구성합니다. 프로필을 완료하면 `personalityType`과 같은 코드의 `personalityResult`를 함께 반환하므로, 클라이언트는 유형명과 문구를 별도로 변환할 필요가 없습니다. 완료 전에는 두 필드 모두 `null`입니다.

### 클라이언트 저장 흐름

1. 각 단계에서 현재까지 선택한 값을 같은 `PUT` API로 보내 draft를 저장합니다.
2. 마지막 음식 취향 선택 후 `complete=true`로 요청합니다.
3. 성공 응답의 `parentDisplayName`과 `personalityResult`로 결과 화면을 구성합니다.
4. 작성 도중 다시 진입하면 조회 응답의 기존 값을 각 단계에 복원합니다.

---

## GET /api/v1/parent-profiles/me

부모 사용자가 본인의 상세 프로필을 조회합니다.

자녀가 호출하면 HTTP `200`, 본문 `status=400`을 반환합니다.

### Response

`PUT /api/v1/parent-profiles/me`와 같은 응답 형태입니다.

---

## GET /api/v1/parent-profiles/{parentUserId}

같은 가족으로 연결된 자녀가 부모님의 상세 프로필을 조회합니다.
부모 본인도 같은 경로로 본인 프로필을 조회할 수 있습니다.

연결되지 않은 사용자가 조회하면 HTTP `200`, 본문 `status=403`을 반환합니다.

### Response

`PUT /api/v1/parent-profiles/me`와 같은 응답 형태입니다.

---

## 추천용 여행 MBTI

현재 구현은 부모님 프로필 완료 시 `docs/policy/parent-travel-mbti.md`의 가중치 정책으로 아래 enum 중 하나를 `personalityType`에 저장합니다. 화면 표시용 보조명(`name`), 별명(`catchphrase`), 설명(`description`)은 `personalityResult`로 반환합니다. 결과 화면 제목은 `catchphrase`와 `name`을 이어 붙여 `쉬엄쉬엄이 최고! 풍경 수집가`처럼 표시합니다. 결과 카드의 테마 칩은 `personalityResult.themes`를 표시 순서대로 사용합니다. 첫 번째가 대표 테마입니다.

| Value | 설명 |
|-------|------|
| `urban_explorer` | 볼 건 다 봐야지! 도시 탐험가 |
| `culture_stroller` | 분위기가 반이지! 문화 산책가 |
| `healing_traveler` | 쉬엄쉬엄이 최고! 풍경 수집가 |
| `heritage_walker` | 아는 만큼 보인다! 시간 여행자 |
| `active_adventurer` | 해봐야 제맛이지! 체험 대장 |
| `local_challenger` | 현지인처럼 즐기자! 골목 탐험가 |

---

## POST /api/v1/parent-profiles/{parentUserId}/requests

자녀가 같은 가족 부모에게 프로필 작성을 요청하는 푸시 알림을 보냅니다. Request Body는 없습니다.

```json
{
  "status": 200,
  "success": true,
  "message": "부모님 프로필 작성 요청 성공",
  "data": null
}
```

- **요청 이력을 저장하지 않습니다.** 호출할 때마다 알림을 보내며 `나중에 하기` 같은 화면 상태는 앱이 관리합니다.
- 부모가 알림을 껐거나 기기 토큰이 없으면 알림이 발송되지 않지만 **요청 자체는 성공으로 응답**합니다.

| 본문 status | 조건 |
|-------------|------|
| `400` | 이미 프로필 작성을 완료한 부모 |
| `401` | access token이 없거나 유효하지 않음 |
| `403` | 자녀가 아니거나 같은 가족이 아님 |
| `404` | 해당 부모를 찾을 수 없음 |

발송 정책은 `docs/policy/push-notification.md`를 따릅니다.
