# API Response Convention

일반 API 응답은 공통 응답 객체 `ApiResponse<T>`로 감싸는 것을 기본 기준으로 합니다. 클라이언트와 합의한 계약에 따라, 서버가 결과를 정상적으로 만든 요청·Validation·인증·정책 오류는 실제 HTTP `200 OK`로 반환하고 본문의 `status`와 `success`로 구분합니다.

구현 위치: `com.msdragon.backend.common.response.ApiResponse`

---

## 성공 응답

데이터가 없는 경우:

```json
{
  "status": 200,
  "success": true,
  "message": "조회 성공"
}
```

데이터가 있는 경우:

```json
{
  "status": 200,
  "success": true,
  "message": "조회 성공",
  "data": {}
}
```

---

## 실패 응답

```json
{
  "status": 400,
  "success": false,
  "message": "요청 값이 입력되지 않았습니다."
}
```

위 예시의 실제 HTTP 상태는 `200 OK`이고, 본문의 `status=400`이 처리 실패 유형을 나타냅니다.

---

## HTTP 상태 코드

| 실제 HTTP | 사용 기준 |
|---|---|
| `200 OK` | 일반·생성 성공, JSON·파라미터·Validation 오류, 인증 실패, 정책 오류 등 서버가 결과를 정상적으로 만든 모든 응답 |
| `404/405/406/415` 등 | 존재하지 않는 URL, 지원하지 않는 HTTP method·media type 등 API 핸들러에 진입하지 못한 기본 HTTP 오류 |
| `500 Internal Server Error` | 예상하지 못한 서버 오류, 필수 설정 누락, 외부 API·PDF 생성 실패 |

API 요청 자체가 서버에 도달하지 않는 네트워크·타임아웃 실패에는 HTTP 응답 자체가 없습니다. 클라이언트는 이 경우를 HTTP `500`과 별도로 처리합니다.

---

## 본문 status

- `status`는 HTTP 상태와 독립적인 정수형 앱 상태 코드입니다.
- 현재는 의미 전달을 위해 `200`, `201`, `400`, `401`, `403`, `404`, `500`을 사용합니다.
- 생성 API는 실제 HTTP `200`이고 성공 본문은 `status=201`, `success=true`입니다.
- 도메인별 세부 비즈니스 코드가 합의되면 HTTP 코드에 묶이지 않고 `status` 값만 확장합니다.

---

## 규칙

- 클라이언트는 응답이 없는 통신 실패와 HTTP `200`이 아닌 라우팅·시스템 오류를 먼저 처리합니다.
- HTTP `200`을 받은 후에는 반드시 `success`와 `status`를 확인해 성공과 처리 실패를 분기합니다.
- `success`는 성공 여부입니다.
- `message`는 사용자 또는 API 소비자가 이해할 수 있는 응답 메시지입니다.
- `data`는 응답 데이터가 있을 때만 포함합니다.
- Health check처럼 단순 상태 확인 API는 예외적으로 전용 응답 DTO를 사용할 수 있습니다.
- Bean Validation 실패는 HTTP `200`, 본문 `status=400`, `success=false`로 반환하며 첫 번째 field error의 `defaultMessage`를 `message`로 내려줍니다.
- validation error의 필드별 상세 목록은 아직 응답에 포함하지 않습니다.
- PDF 등 바이너리 성공 응답을 사용하는 API도 처리 가능한 오류는 HTTP `200`, `Content-Type: application/json`과 위 실패 응답 구조를 사용합니다. 클라이언트는 `Content-Type`으로 PDF와 JSON을 구분합니다.
