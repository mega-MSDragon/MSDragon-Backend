# Trip Pledge API

여행 전 가족이 확인할 여행 10계명 후보 조회와 여행별 확정본 저장을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

## 결정 사항

- 여행을 생성한 자녀만 후보를 조회하고 확정본을 저장할 수 있습니다.
- 작성은 여행 상태가 `planning` 또는 `ready`일 때만 가능합니다.
- 후보 조회는 활성 템플릿 중 중복 없는 10개를 무작위로 반환하며 DB에 선택 결과를 저장하지 않습니다.
- 클라이언트는 수정 중인 문구를 로컬 화면 상태로 유지합니다.
- 본인 서명 화면으로 이동하기 직전에 확정 문구 10개를 저장하며 상태는 `reviewed`가 됩니다.
- `reviewed`까지는 다시 저장할 수 있고, 후속 서명 요청 상태부터는 수정할 수 없습니다.
- 비트맵 서명, 문서 렌더링, PDF 생성은 후속 API에서 구현합니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/trips/{tripId}/pledge/candidates` | 무작위 템플릿 후보 10개 조회 |
| `GET` | `/api/v1/trips/{tripId}/pledge` | 여행별 10계명 확정본 조회 |
| `PUT` | `/api/v1/trips/{tripId}/pledge` | 수정 완료한 10계명 확정본 저장 |

## GET /api/v1/trips/{tripId}/pledge/candidates

활성 템플릿 후보군에서 중복 없이 10개를 무작위로 반환합니다.
확정본이 이미 저장된 여행이면 새 후보를 만들지 않고 `400 Bad Request`를 반환합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 10계명 후보 조회 성공",
  "data": {
    "tripId": 1,
    "candidates": [
      {
        "id": 12,
        "content": "서로 재촉하지 않기"
      }
    ]
  }
}
```

화면을 벗어났다가 확정본 저장 전에 다시 호출하면 이전과 다른 후보 조합이 내려올 수 있습니다.

## PUT /api/v1/trips/{tripId}/pledge

사용자가 수정 완료한 문구 10개를 여행별 확정본으로 저장합니다.
배열 순서가 `sortOrder` 1~10으로 저장됩니다.

### Request

```json
{
  "items": [
    {
      "templateId": 12,
      "content": "서로 서두르지 않기"
    },
    {
      "templateId": null,
      "content": "하루에 한 번 가족사진 찍기"
    }
  ]
}
```

위 예시는 항목 형식만 보여주기 위해 2개로 축약했습니다. 실제 요청의 `items`에는 정확히 10개를 보내야 합니다.

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `items` | array | true | 정확히 10개 |
| `items[].templateId` | number | false | 후보 템플릿 ID. 직접 작성한 항목은 `null` 가능 |
| `items[].content` | string | true | 공백 제외 필수, 최대 255자 |

- 같은 `templateId`를 한 요청에서 중복 사용할 수 없습니다.
- 비활성 또는 존재하지 않는 템플릿 ID는 사용할 수 없습니다.
- 템플릿 문구를 그대로 저장하면 `isFromTemplate=true`, 수정하거나 직접 작성하면 `false`로 저장합니다.
- 기존 상태가 `draft` 또는 `reviewed`이면 10개 항목을 전체 교체합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 10계명 저장 성공",
  "data": {
    "id": 1,
    "tripId": 1,
    "title": "가족 여행 10계명",
    "status": "reviewed",
    "items": [
      {
        "id": 1,
        "sortOrder": 1,
        "templateId": 12,
        "content": "서로 서두르지 않기",
        "isFromTemplate": false
      }
    ],
    "reviewedAt": "2026-07-15T12:00:00",
    "requestedAt": null,
    "completedAt": null,
    "renderedImageUrl": null,
    "pdfUrl": null
  }
}
```

응답 예시의 `items`도 1개만 축약해 표시했으며 실제 응답에는 저장된 10개가 순서대로 포함됩니다.

## GET /api/v1/trips/{tripId}/pledge

저장된 여행별 확정본을 조회합니다. 현재 단계에서는 작성자인 자녀만 조회할 수 있습니다.
부모 조회 권한은 자녀 서명 후 `signature_requested` 전환 API를 구현할 때 확장합니다.

저장된 확정본이 없으면 `404 Not Found`를 반환합니다.
