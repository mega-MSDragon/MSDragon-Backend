# Notification API

FCM 푸시 알림 기기 토큰을 등록·해제합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증 오류도 HTTP `200`으로 반환하며, 아래 실패 코드는 본문 `status`입니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/v1/users/me/device-tokens` | 기기 토큰 등록 또는 갱신 |
| `DELETE` | `/api/v1/users/me/device-tokens` | 기기 토큰 해제 |

---

## POST /api/v1/users/me/device-tokens

로그인 후 FCM 토큰을 받아 호출합니다. **앱 실행마다 호출해도 안전합니다.** 같은 토큰은 갱신만 합니다.

### Request

```json
{
  "token": "fcm-device-token",
  "platform": "ios"
}
```

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `token` | string | true | 최대 512자 |
| `platform` | enum | true | `ios`, `android`, `web` |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "기기 토큰 등록 성공",
  "data": null
}
```

**같은 기기에 다른 사용자가 로그인하면 토큰 소유자를 옮깁니다.** 옮기지 않으면 이전 사용자에게 갈 알림이 새 사용자 기기로 갑니다.

### 오류

| 본문 status | 조건 |
|-------------|------|
| `400` | `token`이 비었거나 512자를 넘음, `platform`이 허용 값이 아님 |
| `401` | access token이 없거나 유효하지 않음 |

---

## DELETE /api/v1/users/me/device-tokens

**로그아웃할 때 호출합니다.** 호출하지 않으면 그 기기를 쓰는 다음 사용자에게 이전 사용자의 알림이 갈 수 있습니다.

### Request

```json
{
  "token": "fcm-device-token"
}
```

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "기기 토큰 해제 성공",
  "data": null
}
```

이미 없는 토큰이나 다른 사용자의 토큰을 보내도 **성공으로 처리**합니다. 로그아웃이 토큰 상태 때문에 실패해서는 안 됩니다.

회원 탈퇴(`DELETE /api/v1/users/me`) 시에는 서버가 해당 사용자의 토큰을 모두 삭제하므로 따로 호출하지 않아도 됩니다.

---

## 발송되는 알림

| 알림 | 트리거 | 대상 | `data.type` |
|------|--------|------|-------------|
| 여행 평가 요청 | `POST /api/v1/trips/{tripId}/feedback/requests` | 새로 요청된 참여 부모 | `trip_feedback_request` (`tripId` 포함) |
| 부모님 프로필 작성 요청 | `POST /api/v1/parent-profiles/{parentUserId}/requests` | 요청 대상 부모 | `parent_profile_request` |

- 알림 본문에는 **개인정보를 넣지 않습니다.** 잠금 화면에 그대로 표시되기 때문입니다. 앱은 `data.type`으로 열 화면을 결정합니다.
- 알림을 끈 사용자(`notificationEnabled=false`)와 기기 토큰이 없는 사용자는 발송 대상에서 제외되지만, **트리거 API는 성공으로 응답합니다.**
- 서버에 Firebase 키가 설정되지 않은 동안에도 트리거 API는 정상 동작하며 발송만 건너뜁니다.

발송 범위와 정책은 `docs/policy/push-notification.md`를 따릅니다.
