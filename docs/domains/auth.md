# Auth Domain

Auth 도메인은 소셜 로그인, 회원가입 완료, 서비스 토큰 발급과 로그아웃을 담당합니다.

---

## 책임

- 카카오 access token으로 카카오 사용자 식별자를 검증합니다.
- 애플 identity token을 Apple JWKS로 검증합니다.
- `oauth_provider + oauth_subject` 기준으로 사용자를 식별합니다.
- 탈퇴 시 기존 소셜 식별자를 익명화하므로 같은 소셜 계정은 새 사용자로 가입할 수 있습니다.
- 애플 로그인 시 함께 받은 `authorizationCode`를 provider refresh token으로 교환해 `users.oauth_refresh_token`에 보관합니다. 탈퇴 시 애플 연결 해제(revoke)에 사용하며, 코드가 일회용이라 로그인 시점에만 확보할 수 있습니다.
- 미가입 사용자는 `users` row가 없으므로 교환한 provider refresh token을 `signupToken` claim으로 전달해 회원가입 완료 시 저장합니다.
- 탈퇴 시 provider 앱 연결을 해제합니다. 애플은 `client_secret` JWT(ES256)와 저장한 refresh token으로 revoke하고, 카카오는 어드민 키로 unlink합니다. 실패는 탈퇴를 막지 않습니다.
- 미가입 사용자는 `signupToken`만 발급하고 DB에 임시 사용자를 만들지 않습니다.
- 회원가입 완료 시 `users`를 생성하고 access/refresh token을 발급합니다.
- 개인정보 수집 및 이용 필수 약관과 위치 기반 편의시설 안내 선택 약관의 결정을 버전별로 저장합니다.
- 회원가입 화면의 성별을 생략하면 `undisclosed`로 저장합니다.
- refresh token은 해시만 저장하고 재발급 시 기존 token을 폐기합니다.
- 로그아웃은 요청받은 refresh token 하나를 폐기하며 이미 폐기되었거나 존재하지 않는 토큰도 성공으로 처리합니다.
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
- `user_consents`
- `user_refresh_tokens`

---

## 외부 연동

| 대상 | 용도 |
|------|------|
| `POST https://appleid.apple.com/auth/token` | `authorizationCode`를 refresh token으로 교환 |
| `POST https://appleid.apple.com/auth/revoke` | 탈퇴 시 애플 앱 연결 해제 |
| `POST https://kapi.kakao.com/v1/user/unlink` | 탈퇴 시 카카오 연결 끊기 |

필수 설정이 비어 있으면 교환과 연결 해제를 조용히 건너뜁니다. 상세 정책은 `docs/policy/account-withdrawal.md`를 따릅니다.

---

## 관련 API

- `POST /api/v1/auth/social-login`
- `POST /api/v1/auth/signup/complete`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

---

## 후속 범위

- 가족 코드 발급
- 가족 코드 매칭
