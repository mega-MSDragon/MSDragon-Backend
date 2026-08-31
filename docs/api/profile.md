# Profile API

마이페이지의 내 기본 프로필 조회, 수정과 회원 탈퇴를 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·인증·정책 오류도 HTTP `200`으로 반환하며, 본문의 `status`와 `success`로 구분합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/users/me` | 내 프로필 조회 |
| `PATCH` | `/api/v1/users/me` | 내 프로필 수정 |
| `DELETE` | `/api/v1/users/me` | 회원 탈퇴 |

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
    "gender": "female",
    "profileImage": "flower"
  }
}
```

`profileImage`는 프리셋 아바타 식별자입니다. 선택 전이거나 지운 상태면 `null`이며 클라이언트가 기본 실루엣을 표시합니다.

---

## PATCH /api/v1/users/me

전달한 필드만 수정합니다.

### Request

```json
{
  "displayName": "최혜린",
  "ageBand": "30s",
  "gender": "female",
  "profileImage": "flower"
}
```

| Field | Type | Required | 허용 값 |
|-------|------|----------|---------|
| `displayName` | string | false | 최대 50자 |
| `ageBand` | enum | false | `10s`, `20s`, `30s`, `40s`, `50s`, `60s`, `60s_plus`, `70s`, `80s`, `90s_plus`, `undisclosed` |
| `gender` | enum | false | `female`, `male`, `undisclosed` |
| `profileImage` | enum | false | `basic`, `flower`, `sunglasses`, `straw_hat`, `none` |

`profileImage`는 필드를 **생략하면 기존 아바타를 유지**하고 `none`을 보내면 **지웁니다**. 응답에는 `none`이 나가지 않고 지운 상태는 `null`입니다. 상세 정책은 `docs/policy/mypage.md`를 따릅니다.

역할별 연령대 검증은 회원가입 완료 API와 같은 기준을 사용합니다.

### Response

`GET /api/v1/users/me`와 같은 형태입니다.

---

## DELETE /api/v1/users/me

현재 로그인 계정을 탈퇴 처리합니다. Request Body는 없습니다.

탈퇴가 완료되면 기존 access token과 refresh token을 사용할 수 없습니다. 같은 소셜 계정으로 다시 로그인하면 미가입 사용자로 처리되며 회원가입을 새로 진행합니다.

서버는 탈퇴 처리와 함께 소셜 로그인 제공자의 앱 연결을 해제합니다(애플 revoke, 카카오 unlink). 이용자의 애플·카카오 계정 자체를 삭제하는 것이 아니라 앱과 계정의 연결만 끊으므로, 재가입 시 동의 화면이 다시 표시됩니다. 연결 해제 실패는 탈퇴 결과에 영향을 주지 않으며 응답도 성공입니다. 클라이언트가 별도로 처리할 것은 없습니다.

### Request

```http
DELETE /api/v1/users/me
Authorization: Bearer {accessToken}
```

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "회원 탈퇴 성공"
}
```

### 오류

| 본문 status | 조건 |
|-------------|------|
| `401` | access token이 없거나 유효하지 않음 |

가족 연결, 여행 기록, 소셜 연결 해제 처리 기준은 `docs/policy/account-withdrawal.md`를 따릅니다.
