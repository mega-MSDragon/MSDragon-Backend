# Auth API

카카오/애플 소셜 로그인과 회원가입 완료를 처리합니다.

---

## 결정 사항

- 카카오는 앱에서 받은 `accessToken`을 백엔드로 전달합니다.
- 애플은 앱에서 받은 `identityToken`을 백엔드로 전달합니다.
- 미가입 사용자는 DB에 임시 `users` row를 만들지 않고 `signupToken`을 발급합니다.
  - 이유: `users.role`은 필수 컬럼이므로 역할 선택 전 row를 만들면 스키마와 충돌합니다.
- 회원가입 완료 시 `users`를 생성하고 access/refresh token을 발급합니다.
- access token 만료 시간은 1시간, refresh token 만료 시간은 14일, signup token 만료 시간은 30분입니다.
- refresh token은 원문을 저장하지 않고 SHA-256 해시로 저장하며, 재발급 시 회전합니다.
- 요청 enum은 API DTO에서 enum 타입으로 받으며 JSON 값은 소문자 문자열을 사용합니다.
- `deviceId`는 현재 요구사항에서 쓰이지 않으므로 받지 않습니다. 기기별 로그아웃/푸시/기기 관리가 필요해질 때 다시 추가합니다.
- 로그인 이후 보호 API는 `Authorization: Bearer {accessToken}` 헤더로 인증합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/v1/auth/social-login` | 소셜 토큰 검증 및 가입 상태 확인 |
| `POST` | `/api/v1/auth/signup/complete` | 회원가입 완료 및 서비스 토큰 발급 |
| `POST` | `/api/v1/auth/refresh` | refresh token 회전 및 토큰 재발급 |

---

## 인증 헤더

`/api/v1/auth/**`를 제외한 보호 API는 access token을 Bearer 형식으로 전달해야 합니다.

```http
Authorization: Bearer {accessToken}
```

access token이 없거나, 형식이 다르거나, 만료/변조된 경우 `401` 공통 실패 응답을 반환합니다.

---

## POST /api/v1/auth/social-login

### Request

```json
{
  "provider": "kakao",
  "token": "kakao-access-token",
  "platform": "android"
}
```

요청 값:

| Field | Type | Required | 허용 값 | 설명 |
|-------|------|----------|---------|------|
| `provider` | enum | true | `kakao`, `apple` | 소셜 로그인 종류 |
| `token` | string | true | - | Kakao `accessToken` 또는 Apple `identityToken` |
| `platform` | enum | false | `ios`, `android`, `web` | 요청 앱 플랫폼. 통계/디버깅용 선택 값 |

### Response: 미가입

```json
{
  "status": 200,
  "success": true,
  "message": "소셜 로그인 처리 성공",
  "data": {
    "signupRequired": true,
    "signupToken": "..."
  }
}
```

### Response: 가입 완료 사용자

```json
{
  "status": 200,
  "success": true,
  "message": "소셜 로그인 처리 성공",
  "data": {
    "signupRequired": false,
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "accessTokenExpiresInSeconds": 3600,
    "refreshTokenExpiresInSeconds": 1209600,
    "user": {
      "id": 1,
      "role": "child",
      "displayName": "최혜린",
      "ageBand": "20s",
      "gender": "female",
      "signupCompleted": true
    }
  }
}
```

---

## POST /api/v1/auth/signup/complete

### Request

```json
{
  "signupToken": "...",
  "role": "child",
  "displayName": "최혜린",
  "ageBand": "20s",
  "gender": "female",
  "platform": "android"
}
```

요청 enum 값:

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `role` | enum | true | `child`, `parent` |
| `ageBand` | enum | true | `10s`, `20s`, `30s`, `40s`, `50s`, `60s`, `60s_plus`, `70s`, `80s`, `90s_plus`, `undisclosed` |
| `gender` | enum | true | `female`, `male`, `undisclosed` |
| `platform` | enum | false | `ios`, `android`, `web` |

역할별 연령대 검증:

| role | 허용 ageBand |
|------|--------------|
| `child` | `10s`, `20s`, `30s`, `40s`, `50s`, `60s_plus`, `undisclosed` |
| `parent` | `50s`, `60s`, `70s`, `80s`, `90s_plus`, `undisclosed` |

### Response

```json
{
  "status": 201,
  "success": true,
  "message": "회원가입 완료",
  "data": {
    "signupRequired": false,
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "accessTokenExpiresInSeconds": 3600,
    "refreshTokenExpiresInSeconds": 1209600,
    "user": {
      "id": 1,
      "role": "child",
      "displayName": "최혜린",
      "ageBand": "20s",
      "gender": "female",
      "signupCompleted": true
    }
  }
}
```

---

## POST /api/v1/auth/refresh

### Request

```json
{
  "refreshToken": "..."
}
```

### Response

`/social-login`의 가입 완료 사용자 응답과 같은 형태입니다.
