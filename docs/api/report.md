# Report API

참여 부모의 피드백을 집계한 여행별 효도 리포트와 완료·중단 여행 기록을 조회합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 인증·정책 오류도 HTTP `200`으로 반환하며, 아래 실패 코드는 본문 `status`입니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/records` | 기록 탭 완료·중단 여행 목록과 완료 여행 통계 조회 |
| `POST` | `/api/v1/trips/{tripId}/filial-report` | 효도 리포트 생성 또는 기존 리포트 반환 |
| `GET` | `/api/v1/trips/{tripId}/filial-report` | 생성된 효도 리포트 조회 |

## GET /api/v1/records

현재 가족의 `completed`, `stopped` 여행과 사용자가 직접 참여했던 같은 상태의 여행을 합쳐 종료일과 여행 ID 내림차순으로 조회합니다.

- 여행 마지막 날의 `in_progress` 여행은 포함하지 않습니다.
- `stopped` 여행은 목록에 포함하지만 상단 완료 여행 통계와 효도 리포트 대상에서는 제외합니다.
- `reportReady=false`인 여행도 목록에 포함합니다.
- 부모 일부만 피드백을 제출했다면 제출된 값만으로 카드의 현재 평균 만족도를 반환합니다.
- 상단 평균 만족도는 여행별 평균을 동일 비중으로 다시 평균냅니다.
- 방문지 수와 이동거리는 피드백 여부와 관계없이 완료 여행 전체를 집계합니다.
- 현재 가족이 없으면 `familyId=null`입니다. 과거 완료 여행 참여 이력이 있으면 목록과 통계는 계속 반환합니다.

```json
{
  "status": 200,
  "success": true,
  "message": "기록 탭 조회 성공",
  "data": {
    "familyId": 1,
    "statistics": {
      "completedTripCount": 3,
      "averageRating": 4.7,
      "totalPlaceCount": 14,
      "totalDistanceKm": 107.50
    },
    "records": [
      {
        "tripId": 3,
        "title": "부산 온천 가족여행",
        "destination": {
          "code": "busan",
          "displayName": "부산",
          "displayOrder": 4,
          "badgeLabel": null
        },
        "startDate": "2026-07-10",
        "endDate": "2026-07-11",
        "status": "completed",
        "participants": [],
        "coverImageUrl": "https://example.com/place.jpg",
        "totalPlaceCount": 5,
        "averageRating": 4.8,
        "reportReady": true
      }
    ]
  }
}
```

## POST /api/v1/trips/{tripId}/filial-report

모든 참여 부모가 피드백을 제출한 여행의 효도 리포트를 생성합니다.

- Request Body는 없습니다.
- 마지막 부모가 피드백을 제출할 때 서버가 같은 생성 로직을 자동 실행합니다.
- 이미 리포트가 있으면 새 리포트를 만들지 않고 현재 코스 집계값을 반영해 반환합니다.

## GET /api/v1/trips/{tripId}/filial-report

같은 가족 구성원이 생성된 효도 리포트를 조회합니다.

- 대표 이미지, 방문지 수, 평균 별점, 이동거리는 현재 저장 데이터를 기준으로 다시 맞춥니다.
- 생성된 리포트가 없으면 HTTP `200`, 본문 `status=404`를 반환합니다.

## Response

```json
{
  "status": 200,
  "success": true,
  "message": "효도 리포트 조회 성공",
  "data": {
    "id": 1,
    "tripId": 1,
    "title": "부산 온천 가족여행",
    "destination": {
      "code": "busan",
      "displayName": "부산",
      "displayOrder": 4,
      "badgeLabel": null
    },
    "startDate": "2026-07-10",
    "endDate": "2026-07-11",
    "participants": [],
    "coverImageUrl": "https://example.com/place.jpg",
    "totalPlaceCount": 5,
    "averageRating": 4.8,
    "totalDistanceKm": 7.50,
    "goodTags": [
      "walking_comfortable",
      "food_good"
    ],
    "improvementTags": [
      "more_rest_needed"
    ],
    "parentFeedbacks": [
      {
        "parentUserId": 2,
        "displayName": "길순님",
        "relationLabel": "엄마",
        "overallRating": 4.5,
        "bodyCondition": "comfortable",
        "bestPlace": {
          "tripStopId": 15,
          "name": "해운대 해수욕장",
          "imageUrl": "https://example.com/place.jpg"
        },
        "freeComment": "다음에도 함께 여행하고 싶어요.",
        "submittedAt": "2026-07-28T11:00:00"
      }
    ],
    "stops": [
      {
        "tripStopId": 15,
        "dayNumber": 1,
        "sortOrder": 1,
        "name": "해운대 해수욕장",
        "category": "관광지",
        "imageUrl": "https://example.com/place.jpg"
      }
    ],
    "shareImageUrl": null,
    "generatedAt": "2026-07-28T12:00:00"
  }
}
```

## 오류

| 본문 status | 조건 |
|--------|------|
| `400` | 참여 부모 중 아직 피드백을 제출하지 않은 사용자가 있음 |
| `401` | 유효한 액세스 토큰이 없음 |
| `403` | 여행과 다른 가족의 사용자 |
| `404` | 여행 또는 생성된 효도 리포트를 찾을 수 없음 |

상세 정책은 `docs/policy/filial-report.md`를 따릅니다.
