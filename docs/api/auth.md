# Auth API

카카오/애플 소셜 로그인과 회원가입 완료를 처리합니다.

> 응답 규칙: 서버가 처리한 요청·토큰 검증 실패도 HTTP `200`으로 반환하며, 본문의 `status`와 `success`로 구분합니다.

---

## 결정 사항

- 카카오는 앱에서 받은 `accessToken`을 백엔드로 전달합니다.
- 애플은 앱에서 받은 `identityToken`을 백엔드로 전달합니다.
- 미가입 사용자는 DB에 임시 `users` row를 만들지 않고 `signupToken`을 발급합니다.
- 탈퇴한 계정의 소셜 식별자는 익명화되므로 같은 소셜 계정으로 로그인하면 미가입 사용자 응답을 반환합니다.
  - 이유: `users.role`은 필수 컬럼이므로 역할 선택 전 row를 만들면 스키마와 충돌합니다.
- 회원가입 완료 시 `users`를 생성하고 access/refresh token을 발급합니다.
- 개인정보 수집 및 이용 약관은 필수이며, 위치 기반 편의시설 안내 약관은 선택입니다.
- 두 약관의 결정은 서버가 관리하는 버전과 함께 `user_consents`에 저장합니다. 최초 버전은 각각 `v1`입니다.
- 성별을 생략하면 `undisclosed`로 저장합니다.
- 개발 테스트 기간에는 access token 만료 시간을 365일로 설정합니다. `APP_AUTH_ACCESS_TOKEN_EXPIRATION`으로 조정할 수 있으며, refresh token은 14일, signup token은 30분입니다.
- refresh token은 원문을 저장하지 않고 SHA-256 해시로 저장하며, 재발급 시 회전합니다.
- 로그아웃은 현재 세션의 refresh token 하나만 폐기하며, 이미 폐기되었거나 서버에 없는 토큰도 성공으로 처리합니다.
- 로그아웃 후 access token은 만료 전까지 유효하므로 클라이언트가 보관 중인 access/refresh token을 모두 삭제합니다.
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
| `POST` | `/api/v1/auth/logout` | 현재 세션의 refresh token 폐기 |

---

## 인증 헤더

`/api/v1/auth/**`를 제외한 보호 API는 access token을 Bearer 형식으로 전달해야 합니다.

```http
Authorization: Bearer {accessToken}
```

access token이 없거나, 형식이 다르거나, 만료/변조된 경우 HTTP `200`, 본문 `status=401`, `success=false`인 공통 실패 응답을 반환합니다.

---

## POST /api/v1/auth/social-login

### Request

```json
{
  "provider": "apple",
  "token": "apple-identity-token",
  "authorizationCode": "apple-authorization-code",
  "platform": "ios"
}
```

요청 값:

| Field | Type | Required | 허용 값 | 설명 |
|-------|------|----------|---------|------|
| `provider` | enum | true | `kakao`, `apple` | 소셜 로그인 종류 |
| `token` | string | true | - | Kakao `accessToken` 또는 Apple `identityToken` |
| `authorizationCode` | string | false | - | 애플 로그인 시 함께 받은 authorizationCode. 탈퇴 시 애플 연결 해제에 필요합니다. 카카오는 보내지 않습니다 |
| `platform` | enum | false | `ios`, `android`, `web` | 요청 앱 플랫폼. 통계/디버깅용 선택 값 |

`authorizationCode`는 일회용이고 발급 후 5분 안에만 유효하므로 서버가 로그인 시점에 provider refresh token으로 교환해 보관합니다. 탈퇴 시점에는 코드를 다시 받을 수 없습니다.

- iOS는 `ASAuthorizationAppleIDCredential.authorizationCode`(Data)를 UTF-8 문자열로 변환해 전달합니다.
- 교환에 실패해도 로그인은 정상 처리합니다. 다만 그 사용자는 탈퇴 시 애플 연결 해제를 할 수 없습니다.
- 미가입 사용자는 `users` row가 없으므로 교환 결과를 `signupToken`에 담아 회원가입 완료 시 저장합니다. 클라이언트는 `signupToken`을 그대로 다음 단계에 전달하면 됩니다.

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
    "accessTokenExpiresInSeconds": 31536000,
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
  "privacyConsentAgreed": true,
  "locationBasedFacilityConsentAgreed": false,
  "platform": "android"
}
```

요청 enum 값:

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `role` | enum | true | `child`, `parent` |
| `ageBand` | enum | true | `10s`, `20s`, `30s`, `40s`, `50s`, `60s`, `60s_plus`, `70s`, `80s`, `90s_plus`, `undisclosed` |
| `gender` | enum | false | `female`, `male`, `undisclosed`. 생략 시 `undisclosed` |
| `privacyConsentAgreed` | boolean | true | 개인정보 수집 및 이용 필수 약관. `true`만 허용 |
| `locationBasedFacilityConsentAgreed` | boolean | false | 위치 기반 편의시설 안내 선택 약관. 기본값 `false` |
| `platform` | enum | false | `ios`, `android`, `web` |

역할별 연령대 검증:

| role | 허용 ageBand |
|------|--------------|
| `child` | `10s`, `20s`, `30s`, `40s`, `50s`, `60s_plus`, `undisclosed` |
| `parent` | `30s`, `40s`, `50s`, `60s`, `70s`, `80s`, `90s_plus`, `undisclosed` |

앱은 약관·역할·이름·연령대·성별을 화면별로 보관한 뒤 마지막 `가입 완료`에서 이 API를 한 번 호출합니다. 단계별 서버 저장 API는 두지 않습니다.

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
    "accessTokenExpiresInSeconds": 31536000,
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

---

## POST /api/v1/auth/logout

### Request

```json
{
  "refreshToken": "..."
}
```

이미 폐기되었거나 서버에 없는 refresh token도 같은 성공 응답을 반환합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "로그아웃 성공",
  "data": null
}
```

클라이언트는 응답을 받은 뒤 로컬에 저장한 access token과 refresh token을 모두 삭제합니다.

---

## 가입 후 가족 연결

회원가입 완료 후 가족 연결 전에는 홈으로 진입하지 않습니다. 클라이언트는 `GET /api/v1/family`로 연결 여부를 확인하고, `familyId=null`이면 가족 연결 화면을 유지합니다.

가족 연결은 다음 API를 순서에 맞게 사용합니다.

1. `POST /api/v1/family/code`: 내 고정 초대 코드 발급 또는 조회
2. `POST /api/v1/family/matches`: 상대방 코드로 가족 연결
3. `GET /api/v1/family`: 다른 사용자가 내 코드를 입력한 경우 연결 완료 여부 확인
