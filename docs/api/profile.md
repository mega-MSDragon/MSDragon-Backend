# Profile API

마이페이지의 내 기본 프로필 조회/수정을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/users/me` | 내 프로필 조회 |
| `PATCH` | `/api/v1/users/me` | 내 프로필 수정 |

---

## GET /api/v1/users/me

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "내 프로필 조회 성공",
  "data": {
    "id": 1,
    "role": "child",
    "displayName": "최혜린",
    "ageBand": "20s",
    "gender": "female"
  }
}
```

---

## PATCH /api/v1/users/me

전달한 필드만 수정합니다.

### Request

```json
{
  "displayName": "최혜린",
  "ageBand": "30s",
  "gender": "female"
}
```

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `displayName` | string | false | 최대 50자 |
| `ageBand` | enum | false | `10s`, `20s`, `30s`, `40s`, `50s`, `60s`, `60s_plus`, `70s`, `80s`, `90s_plus`, `undisclosed` |
| `gender` | enum | false | `female`, `male`, `undisclosed` |

역할별 연령대 검증은 회원가입 완료 API와 같은 기준을 사용합니다.

### Response

`GET /api/v1/users/me`와 같은 형태입니다.
