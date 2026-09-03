# Place API

여행과 무관하게 장소·축제 상세를 조회합니다. 홈 섹션 카드에서 진입하는 상세 화면이 사용합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증·정책 오류도 HTTP `200`으로 반환하며, 아래 실패 코드는 본문 `status`입니다.

---

## 결정 사항

- 여행 코스의 방문지 상세(`GET /api/v1/trips/{tripId}/places/{contentId}`)는 **여행 권한 검사가 필요해 홈에서 호출할 수 없습니다.** 홈 섹션에는 여행이 없으므로 여행에 종속되지 않은 엔드포인트를 별도로 둡니다.
- **응답 형태는 방문지 상세와 완전히 같습니다.** 클라이언트가 상세 화면 하나를 두 진입점에서 재사용하기 위한 결정이며, 필드를 줄이지 않습니다.
- 조회·결합 로직도 같은 서비스를 공유합니다. `detailCommon2`, `detailIntro2`, `detailImage2`, `detailWithTour2` 결합과 HTML 정제가 그대로 적용됩니다.
- **축제 상세 API를 따로 두지 않습니다.** 축제도 TourAPI 콘텐츠(`contentTypeId=15`)라 같은 엔드포인트로 조회합니다.
- 무장애 원문에서 값이 비어 있는 항목은 **'이용 불가'가 아니라 '정보 미제공'** 입니다. 서버는 빈 값을 그대로 전달하고 표시 여부는 클라이언트가 결정합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/places/{contentId}` | 장소·축제 상세 조회 |

---

## GET /api/v1/places/{contentId}

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `contentTypeId` | string | false | TourAPI 콘텐츠 타입 ID. 홈 섹션 항목은 관광지 `12`, 축제 `15`입니다. 허용 값은 `12`, `14`, `15`, `28`, `38`, `39`이며 생략하면 서버가 원본 응답에서 판단합니다 |

### Response

`GET /api/v1/trips/{tripId}/places/{contentId}`와 같은 형태입니다. 필드 설명은 `docs/api/trip.md`의 방문지 상세 조회를 따릅니다.

주요 필드:

| Field | 설명 |
|-------|------|
| `externalPlaceId` | TourAPI contentId |
| `name`, `address`, `latitude`, `longitude` | 제목과 위치 |
| `overview` | 소개. HTML을 제거한 평문입니다 |
| `imageUrl`, `imageUrls[]` | 대표 이미지와 추가 이미지. 0장일 수 있습니다 |
| `operatingHours`, `closedDays`, `admissionFee` | 운영 정보. 원본에 값이 없으면 `null` |
| `phone`, `homepageUrl` | 연락처. 원본 HTML에서 URL만 추출합니다 |
| `accessibility` | 무장애 원문. 값이 비어 있으면 정보 미제공입니다 |
| `contentTypeId`, `contentTypeName` | 콘텐츠 분류 |
| `recommendationTags`, `sourcePayload` | 코스 저장 API 입력용 파생 값. 홈에서 진입한 경우 사용하지 않아도 됩니다 |

### 오류

| 본문 status | 조건 |
|-------------|------|
| `400` | `contentId`가 비었거나 지원하지 않는 `contentTypeId` |
| `401` | access token이 없거나 유효하지 않음 |
| `404` | TourAPI에서 해당 콘텐츠를 찾을 수 없음 |
| `500` | TourAPI 호출 실패 (실제 HTTP도 `500`) |

---

## 클라이언트 사용 흐름

1. `GET /api/v1/home/sections`로 섹션과 항목을 받습니다.
2. 항목 카드를 탭하면 `items[].contentId`와 **항목이 들고 있는 `items[].contentTypeId`** 를 그대로 넘겨 이 API를 호출합니다. 섹션 종류를 알 필요가 없습니다.
3. 여행 코스 방문지 상세와 같은 화면을 그대로 사용합니다.

**상세 화면은 고정 레이아웃이 아닌 가변 섹션 구조로 설계합니다.** 원본 API가 값을 채우지 않는 필드가 많아, 빈 필드를 가정한 고정 레이아웃은 빈 블록이 남습니다.
