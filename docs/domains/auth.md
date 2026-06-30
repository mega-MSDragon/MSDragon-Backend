# Auth Domain

Auth 도메인은 소셜 로그인, 회원가입 완료, 서비스 토큰 발급을 담당합니다.

---

## 책임

- 카카오 access token으로 카카오 사용자 식별자를 검증합니다.
- 애플 identity token을 Apple JWKS로 검증합니다.
- `oauth_provider + oauth_subject` 기준으로 사용자를 식별합니다.
- 미가입 사용자는 `signupToken`만 발급하고 DB에 임시 사용자를 만들지 않습니다.
- 회원가입 완료 시 `users`를 생성하고 access/refresh token을 발급합니다.
- refresh token은 해시만 저장하고 재발급 시 기존 token을 폐기합니다.
- API 요청 enum은 DTO에서 직접 enum 타입으로 받고, JSON 값은 `kakao`, `child`, `20s`처럼 DB 저장 값과 같은 소문자 문자열을 사용합니다.
- `deviceId`는 현재 인증 흐름에서 쓰지 않으므로 받지 않습니다.
- `/api/v1/auth/**`를 제외한 보호 API는 `Authorization: Bearer {accessToken}`으로 인증합니다.
- 컨트롤러는 `@CurrentUser AuthenticatedUser` 파라미터로 현재 로그인 사용자 ID와 역할을 받을 수 있습니다.
- 역할별 허용 연령대 검증은 `UserProfilePolicy`에서 관리하고 회원가입/프로필 수정에서 재사용합니다.

---

## 패키지 구조

```text
auth
├── config
├── controller
├── dto
├── entity
├── repository
├── service
└── support
```

---

## 관련 테이블

- `users`
- `user_refresh_tokens`

---

## 관련 API

- `POST /api/v1/auth/social-login`
- `POST /api/v1/auth/signup/complete`
- `POST /api/v1/auth/refresh`

---

## 후속 범위

- 가족 코드 발급
- 가족 코드 매칭
