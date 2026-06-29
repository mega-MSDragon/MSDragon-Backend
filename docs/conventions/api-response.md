# API Response Convention

일반 API 응답은 공통 응답 객체 `ApiResponse<T>`로 감싸는 것을 기본 기준으로 합니다.

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

---

## 규칙

- `status`는 HTTP 상태 코드 숫자입니다.
- `success`는 성공 여부입니다.
- `message`는 사용자 또는 API 소비자가 이해할 수 있는 응답 메시지입니다.
- `data`는 응답 데이터가 있을 때만 포함합니다.
- Health check처럼 단순 상태 확인 API는 예외적으로 전용 응답 DTO를 사용할 수 있습니다.
- Bean Validation 실패는 첫 번째 field error의 `defaultMessage`를 `message`로 내려줍니다.
- validation error의 필드별 상세 목록은 아직 응답에 포함하지 않습니다.
