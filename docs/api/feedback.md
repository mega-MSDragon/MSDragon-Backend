# Trip Feedback API

여행 마지막 날 또는 수동 종료 후 자녀가 부모 평가를 요청하고, 참여 부모가 여행 소감을 제출하는 API입니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증·정책 오류도 HTTP `200`으로 반환하며, 아래 실패 코드는 본문 `status`입니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/v1/trips/{tripId}/feedback/requests` | 미제출 참여 부모 전체에게 평가 요청 |
| `GET` | `/api/v1/trips/{tripId}/feedback/status` | 부모별 요청·제출 현황 조회 |
| `POST` | `/api/v1/trips/{tripId}/feedback/me` | 현재 부모의 피드백 1회 제출 |
| `GET` | `/api/v1/trips/{tripId}/feedback/me` | 현재 부모가 제출한 피드백 조회 |

## POST /api/v1/trips/{tripId}/feedback/requests

여행을 생성한 자녀가 종료일 00:00부터 또는 여행을 수동 종료한 직후, 아직 피드백을 제출하지 않은 참여 부모 모두에게 평가를 요청합니다.

- Request Body는 없습니다.
- 같은 부모에 대한 요청 이력은 한 건만 저장하므로 재호출해도 중복되지 않습니다.
- 이미 제출한 부모는 요청 대상에서 제외합니다.
- 부모는 이 요청이 없어도 직접 피드백을 제출할 수 있습니다.

응답은 `GET /feedback/status`와 같은 형식입니다.

## GET /api/v1/trips/{tripId}/feedback/status

여행을 생성한 자녀와 참여 부모가 현재 평가 가능 여부와 부모별 요청·제출 현황을 조회합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "피드백 제출 현황 조회 성공",
  "data": {
    "tripId": 1,
    "feedbackAvailable": true,
    "totalParentCount": 2,
    "requestedParentCount": 2,
    "submittedParentCount": 1,
    "canRequest": true,
    "canSubmit": false,
    "reportReady": false,
    "parents": [
      {
        "parentUserId": 2,
        "displayName": "길순님",
        "relationLabel": "엄마",
        "requestedAt": "2026-07-26T10:00:00",
        "submittedAt": "2026-07-26T11:00:00"
      },
      {
        "parentUserId": 3,
        "displayName": "길동님",
        "relationLabel": "아빠",
        "requestedAt": "2026-07-26T10:00:00",
        "submittedAt": null
      }
    ]
  }
}
```

| Field | 설명 |
|-------|------|
| `feedbackAvailable` | 서울 날짜가 여행 종료일 이상이거나 상태가 `completed`이고, 여행 상태가 `stopped`가 아닌지 여부 |
| `canRequest` | 현재 사용자가 생성 자녀이고 미제출 부모가 있어 요청 API를 호출할 수 있는지 여부 |
| `canSubmit` | 현재 사용자가 미제출 참여 부모이고 작성 가능 시점인지 여부 |
| `reportReady` | 모든 참여 부모가 제출해 효도 리포트가 자동 생성되었는지 여부 |

## POST /api/v1/trips/{tripId}/feedback/me

여행에 참여한 부모가 종료일 00:00부터 또는 여행이 수동 종료된 직후 본인 피드백을 한 번 제출합니다. 제출 후에는 수정할 수 없습니다.

### Request

```json
{
  "overallRating": 4.5,
  "bodyCondition": "comfortable",
  "goodTags": [
    "walking_comfortable",
    "food_good"
  ],
  "improvementTags": [
    "more_rest_needed"
  ],
  "bestTripStopId": 15,
  "freeComment": "다음에도 함께 여행하고 싶어요."
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `overallRating` | decimal | true | `0.0`~`5.0`, `0.5` 단위 |
| `bodyCondition` | enum | true | `comfortable`, `slightly_tired`, `very_tired` |
| `goodTags` | enum array | false | 좋았던 점 태그, 최대 6개 |
| `improvementTags` | enum array | false | 개선할 점 태그, 최대 4개 |
| `bestTripStopId` | number | true | 해당 여행에 포함된 방문지 ID |
| `freeComment` | string | false | 공백 제외 200자 이내. 공백만 보내면 `null`로 저장 |

`goodTags` 값:

- `walking_comfortable`
- `rest_time_good`
- `scenery_good`
- `transport_comfortable`
- `food_good`
- `seating_sufficient`

`improvementTags` 값:

- `more_rest_needed`
- `many_stairs_or_slopes`
- `long_travel_time`
- `crowded`

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 피드백 제출 성공",
  "data": {
    "id": 1,
    "tripId": 1,
    "parentUserId": 2,
    "overallRating": 4.5,
    "bodyCondition": "comfortable",
    "goodTags": [
      "walking_comfortable",
      "food_good"
    ],
    "improvementTags": [
      "more_rest_needed"
    ],
    "bestPlace": {
      "tripStopId": 15,
      "name": "오도리 공원"
    },
    "freeComment": "다음에도 함께 여행하고 싶어요.",
    "submittedAt": "2026-07-26T11:00:00",
    "reportReady": false
  }
}
```

## GET /api/v1/trips/{tripId}/feedback/me

여행 참여 부모가 본인이 제출한 피드백을 조회합니다.

- 아직 제출하지 않았으면 HTTP `200`, 본문 `status=404`를 반환합니다.
- 응답은 `POST /feedback/me`의 `data`와 같습니다.

## 오류

| 본문 status | 조건 |
|--------|------|
| `400` | 마지막 날 전이면서 수동 종료되지 않은 여행 또는 기존 `stopped` 여행의 요청·제출, 점수 범위/단위 오류, 태그 그룹·중복 오류, 다른 여행 방문지 선택, 중복 제출 |
| `401` | 유효한 액세스 토큰이 없음 |
| `403` | 생성 자녀가 아닌 요청자, 참여 부모가 아닌 제출자·조회자 |
| `404` | 여행 또는 본인이 제출한 피드백을 찾을 수 없음 |

상세 정책은 `docs/policy/trip-feedback.md`를 따릅니다.
