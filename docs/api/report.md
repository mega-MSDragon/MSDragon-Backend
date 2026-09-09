# Report API

참여 부모의 피드백을 집계한 여행별 효도 리포트와 완료·중단 여행 기록을 조회합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 인증·정책 오류도 HTTP `200`으로 반환하며, 아래 실패 코드는 본문 `status`입니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/records` | 기록 탭 완료·중단 여행 목록과 완료 여행 통계 조회 |
| `GET` | `/api/v1/records/{tripId}` | 기록 상세 화면 한 벌 조회 |
| `POST` | `/api/v1/trips/{tripId}/filial-report` | 효도 리포트 생성 또는 기존 리포트 반환 |
| `GET` | `/api/v1/trips/{tripId}/filial-report` | 생성된 효도 리포트 조회 |

## GET /api/v1/records

로그인 사용자가 **참여자로 포함된** `completed`, `stopped` 여행을 종료일과 여행 ID 내림차순으로 조회합니다. 가족 여행 전체가 아니며, 가족 연결이 끊긴 뒤에도 본인이 참여했던 여행은 계속 조회됩니다.

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

## GET /api/v1/records/{tripId}

기록 카드를 눌렀을 때 필요한 값을 **한 번에** 조회합니다. 여행 상세·피드백·효도 리포트를 각각 부르지 않아도 됩니다.

**참여자만 조회할 수 있습니다.** 같은 가족이어도 참여하지 않은 여행은 HTTP `200`, 본문 `status=403`으로 차단합니다.

효도 리포트 본문은 포함하지 않습니다. `보러가기`를 눌렀을 때만 필요하고 응답이 커지기 때문입니다. 그때 `GET /api/v1/trips/{tripId}/filial-report`를 호출합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "기록 상세 조회 성공",
  "data": {
    "tripId": 1,
    "title": "부산 온천 가족여행",
    "coverImageUrl": "https://tong.visitkorea.or.kr/cms/resource/00/1234500_image2_1.jpg",
    "status": "completed",
    "destination": { "code": "busan", "displayName": "부산" },
    "startDate": "2026-09-12",
    "endDate": "2026-09-14",
    "participants": [
      { "userId": 1, "role": "child", "displayName": "혜린", "gender": "female", "relationLabel": null },
      { "userId": 2, "role": "parent", "displayName": "김영희", "gender": "female", "relationLabel": "엄마" },
      { "userId": 3, "role": "parent", "displayName": "김철수", "gender": "male", "relationLabel": "아빠" }
    ],
    "summary": {
      "totalDistanceKm": 120.5,
      "totalPlaceCount": 5,
      "placeCounts": [
        { "category": "관광지", "count": 3 },
        { "category": "음식점", "count": 2 }
      ],
      "averageRating": 4.5,
      "parentRatings": [
        { "parentUserId": 2, "displayName": "김영희", "relationLabel": "엄마", "overallRating": 4.0 },
        { "parentUserId": 3, "displayName": "김철수", "relationLabel": "아빠", "overallRating": 5.0 }
      ]
    },
    "days": [
      {
        "dayNumber": 1,
        "travelDate": "2026-09-12",
        "stops": [
          {
            "tripStopId": 10,
            "sortOrder": 1,
            "name": "대릉원",
            "category": "관광지",
            "note": "엄마가 이번 여행에서 제일 가고 싶어 하던 곳!",
            "latitude": 35.8383,
            "longitude": 129.2126
          }
        ]
      }
    ],
    "pledge": {
      "exists": true,
      "allSigned": true,
      "signedParticipants": [],
      "pendingParticipants": []
    },
    "report": {
      "ready": true,
      "submittedParentCount": 2,
      "totalParentCount": 2,
      "submittedParents": [],
      "pendingParents": []
    },
    "canDelete": true
  }
}
```

### 화면 매핑

| 화면 | 사용 필드 |
|------|-----------|
| 상단 배경과 제목 | `coverImageUrl`, `title` |
| 휴지통 아이콘 | `canDelete`. `false`면 숨깁니다. 삭제는 `DELETE /api/v1/trips/{tripId}` |
| `여행 정보` | `destination.displayName`, `startDate`~`endDate`, `participants` |
| `동행` | `participants[].relationLabel`. 본인은 클라이언트가 `나`로 바꿔 표시합니다 |
| `함께 걸은 길` | `summary.totalDistanceKm`. `null`이면 카드를 숨깁니다 |
| `함께 방문한 장소` | `summary.totalPlaceCount`와 `summary.placeCounts`를 `관광지 3 · 음식점 2`로 이어 붙입니다 |
| `부모님 만족도` | `summary.averageRating`. `null`이면 `-`를 표시합니다. 옆줄은 `summary.parentRatings` |
| 일차 탭 | `days[].dayNumber`, `days[].travelDate` |
| 방문지 카드 | `days[].stops[]`. `note`가 없으면 메모 영역을 숨깁니다 |
| `지도로 보기` | `days[].stops[].latitude`/`longitude`. 좌표가 없는 방문지는 핀을 그리지 않습니다 |
| `여행 10계명` 카드 | `pledge`. `exists=false`면 카드를 숨깁니다 |
| `효도 리포트` 카드 | `report` |

### 여행 10계명 카드 상태

| 상태 | 판단 |
|------|------|
| `가족 모두 서명 완료` + `보러가기` | `pledge.allSigned = true` |
| 미완료 | `pledge.pendingParticipants`로 남은 사람을 표시합니다. 문구는 시안 미정입니다 |

### 효도 리포트 카드 상태

`report.pendingParents`에 **조회자 본인이 있는지**로 버튼이 갈립니다.

| 조회자 | 상태 | 문구 | 버튼 |
|--------|------|------|------|
| 자녀 | `submittedParentCount = 0` | `부모님의 별점이 필요해요` / `두분 다 아직이에요` | `별점 부탁드리기` |
| 자녀 | 일부 제출 | `{미제출 부모}의 별점이 필요해요` / `{제출 부모}는 완료했어요` | `별점 부탁드리기` |
| 자녀 | `ready = true` | `부모님이 모두 별점을 남기셨어요` | `보러가기` |
| 부모 | 본인이 `pendingParents`에 있음 | `별점을 남겨주세요` | `별점 남기기` |
| 부모 | `ready = true` | `부모님이 모두 별점을 남기셨어요` | `보러가기` |

`별점 부탁드리기`는 `POST /api/v1/trips/{tripId}/feedback/requests`, `별점 남기기`는 피드백 작성 화면으로 이동합니다.

---

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
