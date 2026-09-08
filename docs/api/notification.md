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

## POST /api/v1/users/me/device-tokens/test

**내 기기로만** 테스트 알림을 보냅니다. 다른 사용자를 대상으로 지정할 수 없어 남용 여지가 없습니다. 실제 알림과 같은 형태의 페이로드를 보내므로 클라이언트가 수신과 화면 이동을 확인할 수 있습니다.

### Request

Body는 생략할 수 있습니다.

```json
{
  "type": "trip_feedback_request",
  "tripId": "12"
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `type` | string | false | `data.type`에 넣을 값. 생략하면 `test`를 보내 앱의 '모르는 type은 홈으로' 처리를 확인할 수 있습니다 |
| `tripId` | string | false | `data.tripId`에 넣을 값. 생략하면 넣지 않습니다 |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "테스트 알림 발송 처리 완료",
  "data": {
    "deviceCount": 1,
    "pushConfigured": true,
    "notificationEnabled": true,
    "attempted": true
  }
}
```

**알림이 오지 않을 때 이 응답으로 원인을 좁힙니다.**

| 값 | 의미 |
|----|------|
| `deviceCount: 0` | 기기 토큰이 등록되지 않았습니다. 등록 API를 먼저 호출합니다 |
| `pushConfigured: false` | 서버에 Firebase 키가 설정되지 않았습니다 |
| `notificationEnabled: false` | 내 알림 설정이 꺼져 있습니다 |
| `attempted: true`인데 알림이 오지 않음 | FCM 발송 단계 문제입니다. 서버 로그의 `errorCode`를 확인합니다 |

---

## 클라이언트가 받는 페이로드

서버는 `notification`(title, body)과 `data`만 보내고 플랫폼별 설정은 지정하지 않습니다.

### iOS (`userInfo`)

```json
{
  "aps": {
    "alert": {
      "title": "여행은 어떠셨어요?",
      "body": "경주 여행 후기를 남겨주세요."
    }
  },
  "type": "trip_feedback_request",
  "tripId": "12"
}
```

`data` 키가 `aps`와 같은 레벨에 붙습니다.

### Android (`RemoteMessage`)

```kotlin
remoteMessage.notification?.title
remoteMessage.notification?.body
remoteMessage.data["type"]
remoteMessage.data["tripId"]
```

`data` 값은 **모두 문자열**입니다. `tripId`도 `"12"` 형태입니다.

### 화면 이동

서버는 무슨 일이 일어났는지만 보내고 **어느 화면으로 갈지는 클라이언트가 결정**합니다. 딥링크를 쓰지 않습니다.

| `data.type` | 화면 |
|-------------|------|
| `trip_feedback_request` (+ `tripId`) | 피드백 작성 |
| `parent_profile_request` | 부모님 프로필 작성 |
| `pledge_signature_request` (+ `tripId`) | 여행 10계명 서명 |
| 그 외 · 모르는 값 | **홈** |

**모르는 `type`은 홈으로 보냅니다.** 서버가 알림 종류를 추가했을 때 구버전 앱이 멈추지 않게 하기 위함입니다.

### 클라이언트 주의사항

- **Android 백그라운드·종료 상태에서는 `onMessageReceived`가 호출되지 않습니다.** 시스템이 트레이에 자동 표시하고, 탭하면 `data`가 런처 Intent의 extras로 전달됩니다. 탭 처리는 Intent extras에서 읽습니다. 포그라운드일 때만 `onMessageReceived`가 호출됩니다.
- **iOS는 소리가 없습니다.** 서버가 `sound`를 지정하지 않습니다. 필요하면 서버에 `ApnsConfig` 추가를 요청합니다. 포그라운드 배너는 `willPresent`에서 presentation option을 반환해야 표시됩니다.
- **Android 매니페스트에 기본 알림 채널과 아이콘을 선언해야 합니다.** `com.google.firebase.messaging.default_notification_channel_id`와 `default_notification_icon`이 없으면 Android 8 이상에서 알림이 표시되지 않거나 아이콘이 흰 사각형으로 보입니다.
- **콜드 스타트를 고려합니다.** 알림 탭으로 앱이 처음 실행되면 로그인 세션이 준비되지 않은 상태입니다. 알림 데이터를 보관해두고 인증과 초기 로딩이 끝난 뒤 화면을 엽니다.

---

## 발송되는 알림

| 알림 | 트리거 | 대상 | `data.type` |
|------|--------|------|-------------|
| 여행 평가 요청 | `POST /api/v1/trips/{tripId}/feedback/requests` | 새로 요청된 참여 부모 | `trip_feedback_request` (`tripId` 포함) |
| 부모님 프로필 작성 요청 | `POST /api/v1/parent-profiles/{parentUserId}/requests` | 요청 대상 부모 | `parent_profile_request` |
| 여행 10계명 서명 요청 | `POST /api/v1/trips/{tripId}/pledge/signatures/me` (자녀 서명) | 아직 서명하지 않은 참여 부모 | `pledge_signature_request` (`tripId` 포함) |

- 알림 본문에는 **개인정보를 넣지 않습니다.** 잠금 화면에 그대로 표시되기 때문입니다. 앱은 `data.type`으로 열 화면을 결정합니다.
- 알림을 끈 사용자(`notificationEnabled=false`)와 기기 토큰이 없는 사용자는 발송 대상에서 제외되지만, **트리거 API는 성공으로 응답합니다.**
- 서버에 Firebase 키가 설정되지 않은 동안에도 트리거 API는 정상 동작하며 발송만 건너뜁니다.

발송 범위와 정책은 `docs/policy/push-notification.md`를 따릅니다.
