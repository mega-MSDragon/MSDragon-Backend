# 회원 탈퇴 정책

회원 탈퇴 API의 개인정보 익명화, 인증 폐기, 가족 연결과 여행 기록 처리 기준을 정의합니다.

## API

- `DELETE /api/v1/users/me`
- Authorization Bearer access token이 필요합니다.
- Request Body는 없습니다.
- 성공 시 실제 HTTP `200`, 본문 `status=200`을 반환합니다.

## 사용자와 인증

- `users` row는 참조 무결성을 위해 물리 삭제하지 않고 `deleted_at`을 기록합니다.
- `oauth_subject`는 `withdrawn:{userId}:{UUID}` 형태의 고유 문자열로 교체합니다.
- `display_name`은 `탈퇴한 사용자`, `age_band`와 `gender`는 `undisclosed`로 익명화합니다.
- `signup_completed_at`과 `last_login_at`은 `null`로 변경합니다.
- 아직 폐기되지 않은 refresh token은 모두 `revoked_at`을 기록합니다.
- 기존 access token은 인증 과정에서 활성 사용자 조회에 실패하므로 탈퇴 직후부터 사용할 수 없습니다.
- 기존 소셜 식별자를 해제하므로 같은 소셜 계정으로 다시 가입하면 새로운 `users` row를 생성합니다. 이전 계정이나 가족 연결은 복구하지 않습니다.
- `oauth_refresh_token`과 `profile_image`는 `null`로 지웁니다.

## 소셜 연결 해제 (revoke / unlink)

탈퇴 시 provider 쪽 앱 연결도 함께 해제합니다. 애플은 App Store Review 5.1.1(v)가 요구하는 항목입니다.

**연결 해제는 사용자의 애플 계정이나 카카오 계정을 삭제하는 것이 아닙니다.** 우리 앱과 그 계정의 연결만 끊습니다. 결과는 다음과 같습니다.

- 사용자의 Apple ID 설정과 카카오계정 연결 목록에서 `모셔용`이 사라집니다.
- 서버가 보유한 provider 접근 권한이 사라집니다.
- 다시 가입하면 최초 로그인처럼 동의 화면이 다시 표시됩니다.
- provider가 발급하는 사용자 식별자(`sub`, 카카오 회원번호)는 같은 값이 다시 올 수 있으나, 탈퇴 시 `oauth_subject`를 익명값으로 교체했으므로 항상 새 `users` row로 가입됩니다.

| Provider | 호출 | 인증 방식 | 앱 변경 |
|----------|------|-----------|---------|
| 애플 | `POST https://appleid.apple.com/auth/revoke` | 팀 키로 서명한 ES256 `client_secret` JWT + 저장한 refresh token | 필요 (`authorizationCode` 전달) |
| 카카오 | `POST https://kapi.kakao.com/v1/user/unlink` | 어드민 키 + `target_id_type=user_id`, `target_id={oauth_subject}` | 불필요 |

카카오는 어드민 키로 서버가 단독 처리하므로 사용자 access token을 저장하지 않습니다.

### 실패를 허용하는 이유

연결 해제 실패는 **탈퇴를 막지 않습니다.** best-effort로 호출하고 실패는 `error` 로그로 남겨 수동 후속 처리합니다.

- 이 기능 이전에 가입한 사용자는 저장된 refresh token이 없어 애플 연결 해제가 불가능합니다. 실패로 막으면 탈퇴 자체가 불가능해집니다.
- 사용자가 이미 iOS 설정에서 직접 연결을 끊었으면 애플이 오류를 반환합니다.
- 외부 장애로 탈퇴가 막히면 개인정보 삭제 요구를 거부하는 셈이 됩니다.

필수 설정(`APP_AUTH_APPLE_TEAM_ID`, `APP_AUTH_APPLE_KEY_ID`, `APP_AUTH_APPLE_PRIVATE_KEY`, `APP_AUTH_KAKAO_ADMIN_KEY`)이 없으면 연결 해제를 조용히 건너뜁니다. 키 발급 전에도 배포할 수 있습니다.

## 가족 연결

- 탈퇴 사용자의 가족 코드는 `is_active=false`로 변경합니다.
- 부모가 탈퇴하면 해당 부모의 `family_members` row만 삭제합니다. 남은 가족은 유지되며 다른 부모 한 명을 다시 연결할 수 있습니다.
- 대표 자녀가 탈퇴하면 가족의 `is_active=false`를 기록하고 구성원의 `family_members` row를 모두 삭제합니다.
- 대표 자녀와 연결이 끊긴 부모는 다른 가족과 다시 매칭할 수 있습니다.
- `family_code_usages`는 과거 매칭 이력으로 유지합니다.

## 여행 기록

- 대표 자녀가 탈퇴할 때 종료일이 지난 여행은 날짜 기준으로 `completed` 상태를 확정합니다.
- `completed`가 아닌 여행은 `archived`로 변경합니다.
- 여행, 참여자, 10계명과 서명, 피드백, 효도 리포트는 과거 기록의 참조 무결성을 위해 유지합니다.
- 현재 가족 연결이 끊겨도 `completed` 여행의 `trip_participants`에 포함된 활성 사용자는 해당 여행과 효도 리포트를 계속 조회할 수 있습니다.
- `archived` 여행은 과거 참여자라는 이유만으로 조회할 수 없습니다.
- 기록 탭은 현재 가족의 완료 여행과 사용자가 직접 참여했던 완료 여행을 합쳐 중복 없이 반환합니다. `familyId`는 현재 활성 가족이 없으면 `null`입니다.

## 트랜잭션

사용자 익명화, 토큰 폐기, 가족 연결 해제와 여행 상태 변경은 하나의 트랜잭션에서 처리합니다. 중간 단계에서 실패하면 전체 변경을 롤백합니다.

소셜 연결 해제는 같은 트랜잭션의 마지막 단계에서 호출하지만 예외를 삼켜 롤백을 유발하지 않습니다. 익명화가 `oauth_subject`와 `oauth_refresh_token`을 덮어쓰므로 해제에 필요한 정보는 트랜잭션 시작 시점에 미리 확보합니다. 외부 호출 타임아웃은 `APP_AUTH_OAUTH_REQUEST_TIMEOUT`(기본 5초)로 제한합니다.
