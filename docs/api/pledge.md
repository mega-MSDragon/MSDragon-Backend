# Trip Pledge API

여행 전 가족이 확인할 여행 10계명 화면, 저장 문구와 참여자 서명을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증·정책 오류도 HTTP `200`으로 반환하며, 문서의 `400/401/403/404`는 본문 `status`입니다. PDF 생성 실패는 실제 HTTP `500`입니다.

## 결정 사항

- 여행을 생성한 자녀만 저장 전 후보를 조회하고 문구를 저장할 수 있습니다.
- 작성은 여행 상태가 `planning` 또는 `ready`일 때만 가능합니다.
- 화면 조회의 저장 전 후보는 활성 템플릿 중 여행별로 고정한 10개를 반환하며 조회만으로 DB에 저장하지 않습니다.
- 클라이언트는 수정 중인 문구를 로컬 화면 상태로 유지합니다.
- 본인 서명 화면으로 이동하기 직전에 문구 10개를 저장하며 내부 상태는 `reviewed`가 됩니다.
- `reviewed`까지는 다시 저장할 수 있고, 후속 서명 요청 상태부터는 수정할 수 없습니다.
- 자녀가 먼저 서명하면 참여 부모에게 조회와 서명을 허용합니다.
- 자녀와 참여 부모 최소 1명이 서명하면 상태는 `completed`가 됩니다.
- `completed` 이후에도 아직 서명하지 않은 다른 참여 부모가 추가로 서명할 수 있습니다.
- 모든 여행 참여자는 현재까지 제출된 전체 서명을 동일하게 조회합니다.
- 화면 응답의 `signers`에는 서명 여부와 관계없이 참여 부모 전체와 작성 자녀가 포함됩니다. 부모 2명 화면도 이 목록으로 구성합니다.
- 서명은 PNG Base64로 받고 디코딩한 원본 바이트를 DB에 저장합니다.
- 여행 수정으로 참여 부모 구성이 바뀌면 저장 문구와 모든 서명을 삭제합니다. 이후 조회에서는 새 참여자 기준 후보 10개를 반환합니다.
- 저장된 10계명은 현재 서명 현황과 함께 HTML에 합성해 요청 시 PDF로 생성하며 완성 파일은 저장하지 않습니다.
- PDF는 계약 번호 `작성 연도-여행 10계명 ID`, 작성일, `우리 가족은`으로 시작하는 공동 서약 문구와 제출된 전체 서명을 포함합니다.

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/trips/{tripId}/pledge/candidates` | 미사용(deprecated) 후보 조회 API |
| `GET` | `/api/v1/trips/{tripId}/pledge` | 여행 10계명 화면 조회 |
| `GET` | `/api/v1/trips/{tripId}/pledge/pdf` | 저장된 10계명 PDF 조회 |
| `PUT` | `/api/v1/trips/{tripId}/pledge` | 수정한 10계명 저장 |
| `POST` | `/api/v1/trips/{tripId}/pledge/signatures/me` | 현재 사용자의 서명 저장 |

## GET /api/v1/trips/{tripId}/pledge/candidates

> 미사용 API입니다. 신규 클라이언트는 `GET /api/v1/trips/{tripId}/pledge`를 사용합니다.

활성 템플릿 후보군에서 중복 없이 10개를 무작위로 반환합니다.
10계명이 이미 저장된 여행이면 새 후보를 만들지 않고 HTTP `200`, 본문 `status=400`을 반환합니다.

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

화면을 벗어났다가 문구 저장 전에 다시 호출하면 이전과 다른 후보 조합이 내려올 수 있습니다.

## PUT /api/v1/trips/{tripId}/pledge

사용자가 수정한 문구 10개를 여행별로 저장합니다.
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
    "canSign": true,
    "signatures": [],
    "signers": [
      {
        "userId": 2,
        "role": "parent",
        "relationLabel": "엄마",
        "displayName": "김지영",
        "signed": false,
        "signatureImageMimeType": null,
        "signatureImageBase64": null,
        "signedAt": null
      },
      {
        "userId": 1,
        "role": "child",
        "relationLabel": "자녀",
        "displayName": "김하늘",
        "signed": false,
        "signatureImageMimeType": null,
        "signatureImageBase64": null,
        "signedAt": null
      }
    ]
  }
}
```

응답 예시의 `items`도 1개만 축약해 표시했으며 실제 응답에는 저장된 10개가 순서대로 포함됩니다.

## GET /api/v1/trips/{tripId}/pledge

여행 10계명 화면을 구성하는 문구와 전체 서명 대상자를 조회합니다.

- 저장된 10계명이 없으면 `id`와 `items[].id`가 `null`인 무작위 템플릿 후보 10개를 반환합니다.
- 저장 전 후보는 조회만으로 DB에 저장되지 않지만 여행별로 고정되어 다시 조회해도 같은 조합과 순서가 유지됩니다.
- 저장 전에도 작성 자녀의 `canSign=true`이며, `signatures=[]`, `signers`에는 모든 참여자가 `signed=false`로 포함됩니다.
- 기본 문구를 수정하지 않은 자녀는 별도 `PUT` 없이 바로 서명 API를 호출할 수 있습니다. 서버가 화면에 표시한 기본 후보 10개를 자동 저장한 뒤 서명을 처리합니다.
- 문구를 하나라도 수정했다면 서명 전에 현재 `items`를 `PUT /pledge`로 저장해야 수정 내용이 반영됩니다.
- 저장 후에는 사용자가 수정해 저장한 문구와 현재까지 제출된 전체 참여자 서명을 반환합니다.
- 응답에 10계명 내부 진행 상태는 노출하지 않습니다.

- 작성 자녀는 저장 전부터 여행 종료 후까지 조회할 수 있습니다.
- 참여 부모는 자녀 서명 후 `signature_requested` 또는 `completed` 상태에서 조회할 수 있습니다.
- 자녀와 모든 참여 부모에게 동일한 `signatures` 목록을 반환합니다.
- `signatures`에는 제출된 서명만 자녀 우선, 부모 서명 시각순으로 포함됩니다.
- `signers`에는 서명 전 참여자까지 모두 포함되며 부모를 참여자 순서대로 먼저, 작성 자녀를 마지막에 반환합니다.
- `signers[].signed=false`이면 서명 이미지와 서명 시간은 `null`입니다. 클라이언트는 해당 칸을 `서명 전`으로 표시합니다.
- `canSign`은 현재 사용자가 아직 본인 서명을 제출할 수 있는지를 나타냅니다.

### 저장 전 응답 예시

```json
{
  "status": 200,
  "success": true,
  "message": "여행 10계명 조회 성공",
  "data": {
    "id": null,
    "tripId": 1,
    "title": "가족 여행 10계명",
    "items": [
      {
        "id": null,
        "sortOrder": 1,
        "templateId": 12,
        "content": "서로 재촉하지 않기",
        "isFromTemplate": true
      }
    ],
    "reviewedAt": null,
    "requestedAt": null,
    "completedAt": null,
    "canSign": true,
    "signatures": [],
    "signers": []
  }
}
```

예시의 `items`와 `signers`는 축약했으며 실제 응답에는 후보 10개와 전체 서명 대상자가 포함됩니다.

## POST /api/v1/trips/{tripId}/pledge/signatures/me

현재 로그인 사용자의 서명을 저장합니다. 제출한 서명은 수정하거나 덮어쓸 수 없습니다.

### Request

```json
{
  "signatureImageBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB..."
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `signatureImageBase64` | string | true | data URI prefix 없는 PNG Base64 문자열 |

- `data:image/png;base64,` prefix는 보내지 않습니다.
- Base64 디코딩 결과는 최대 512KB입니다.
- PNG 파일 시그니처가 아닌 데이터는 거부합니다.
- 자녀가 `reviewed` 상태에서 먼저 서명하면 상태가 `signature_requested`로 변경됩니다.
- 참여 부모가 처음 서명하면 상태가 `completed`로 변경되고 `completedAt`이 기록됩니다.
- 다른 참여 부모가 이후 추가 서명해도 최초 `completedAt`은 변경하지 않습니다.

### Response

응답은 `GET /api/v1/trips/{tripId}/pledge`와 같은 `TripPledgeResponse`입니다. 내부 진행 `status`는 노출하지 않으며 `signatures`에는 제출된 서명을, `signers`에는 서명 전 참여자를 포함한 전체 상태를 표시합니다.

## GET /api/v1/trips/{tripId}/pledge/pdf

저장된 여행 10계명 문구 10개와 현재까지 제출된 전체 참여자 서명을 모바일 서약서 화면과 같은 `374×886px` 비율의 단일 페이지 PDF로 반환합니다.

- 여행 참여자만 조회할 수 있습니다.
- 10계명 문구는 먼저 `PUT /pledge`로 저장되어 있어야 합니다.
- 서명 전에도 PDF를 생성할 수 있으며 미서명자는 `서명 전` 빈 칸으로 표시합니다.
- 자녀와 참여 부모 최소 1명이 서명한 완료 상태에서만 `서약 완료` 도장을 표시합니다.
- 부모 영역은 왼쪽, 자녀 영역은 오른쪽에 표시합니다. 참여 부모가 2명이면 부모 영역을 두 칸으로 나눕니다.
- 완료 조건 충족 후 아직 서명하지 않은 다른 참여 부모도 이름과 `서명 전` 칸으로 표시하며, 추가 서명하면 다음 PDF 요청부터 이미지로 교체됩니다.
- PDF에는 Pretendard 글꼴, 한글 순번, 원형 `서약 완료` 도장과 점선 서명 박스를 사용합니다.
- 계약 번호는 문구 저장 연도와 여행 10계명 ID로 계산하며 별도 DB 컬럼에 저장하지 않습니다.
- 응답 시점에 PDF를 생성하며 DB나 파일 저장소에 완성 파일을 보관하지 않습니다.
- 일반 API와 달리 성공 응답은 `ApiResponse` JSON이 아니라 PDF 원본 바이트입니다.

### Response headers

| Header | Value | 설명 |
|--------|-------|------|
| `Content-Type` | `application/pdf` | PDF 원본 바이트 |
| `Content-Disposition` | `inline; filename="trip-pledge-{tripId}.pdf"` | 앱 내 미리보기를 우선하고 저장 시 사용할 파일명 제공 |
| `Cache-Control` | `private, no-store` | 서명 문서가 공유 캐시에 남지 않도록 제한 |

오류 응답은 기존 공통 JSON 오류 형식을 사용합니다.

| 실제 HTTP | 본문 status | 조건 |
|---|---|---|
| `200` | `401` | 유효한 액세스 토큰이 없음 |
| `200` | `403` | 여행 비참여자가 요청함 |
| `200` | `404` | 여행 또는 저장된 10계명이 없음 |
| `500` | `500` | HTML 합성 또는 PDF 변환 실패 |

### 클라이언트 사용

- Android는 응답 바이트를 앱 캐시의 PDF 파일로 기록한 뒤 `PdfRenderer`로 페이지를 `Bitmap`에 렌더링해 `ImageView`에 표시할 수 있습니다. 같은 임시 파일을 저장 또는 공유 흐름에도 사용합니다.
- iOS는 응답 `Data`로 `PDFDocument(data:)`를 만들고 PDFKit의 `PDFView`에 표시할 수 있습니다. 같은 데이터를 파일로 기록해 공유 시트 또는 저장 기능에 전달합니다.
- PDF 바이트를 Base64나 JSON으로 다시 감싸지 말고 binary 응답으로 받아야 합니다.
