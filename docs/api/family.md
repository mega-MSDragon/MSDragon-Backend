# Family API

가족 코드 발급과 부모-자녀 가족 매칭을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·인증·정책 오류도 HTTP `200`으로 반환하며, 아래 실패 코드는 본문 `status`입니다.

---

## 결정 사항

- 가족 코드는 사용자별 고정 코드입니다.
- 코드 형식은 `MSH-0000` 패턴을 사용하며, 생성 시 충돌을 재시도합니다.
- 매칭 요청은 하이픈을 포함한 `MSH-0001`과 입력 UI의 7자리 값 `MSH0001`을 모두 허용하고 서버에서 저장 형식으로 정규화합니다.
- 매칭은 부모-자녀 조합만 허용합니다.
- 한 사용자는 하나의 가족에만 속할 수 있습니다.
- 가족당 자녀는 1명, 부모는 최대 2명까지 연결합니다.
- 자녀 쪽 가족을 기준으로 가족을 만들고, `families.owner_user_id`는 대표 자녀입니다.
- 가족 구성원 응답의 `relationLabel`은 클라이언트 요청값이 아니라 부모 성별로 서버에서 계산합니다. `female`은 `엄마`, `male`은 `아빠`, `undisclosed`는 `null`입니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/family` | 내 가족과 구성원 조회 |
| `POST` | `/api/v1/family/code` | 내 가족 코드 발급/조회 |
| `POST` | `/api/v1/family/matches` | 상대방 코드로 가족 매칭 |
| `DELETE` | `/api/v1/dev/family` | **[개발용]** 내 가족 연결 해제 |

---

## GET /api/v1/family

마이페이지의 `부모님 프로필`·`자녀 프로필` 카드도 이 API를 사용합니다. `members[].profileCompleted`가 `false`면 `프로필 미입력`을 표시하고, `members[].personalityResult.name`을 여행 MBTI 배지에 씁니다. 자녀는 여행 MBTI가 없어 `profileCompleted`가 항상 `false`, `personalityResult`가 `null`입니다.

### Response: 매칭 전

```json
{
  "status": 200,
  "success": true,
  "message": "내 가족 조회 성공",
  "data": {
    "familyId": null,
    "myCode": "MSH-2405",
    "members": []
  }
}
```

### Response: 매칭 후

```json
{
  "status": 200,
  "success": true,
  "message": "내 가족 조회 성공",
  "data": {
    "familyId": 1,
    "myCode": "MSH-2405",
    "members": [
      {
        "userId": 2,
        "role": "child",
        "displayName": "혜린",
        "ageBand": "20s",
        "gender": "female",
        "relationLabel": null,
        "profileImage": "green",
        "profileCompleted": false,
        "personalityResult": null
      },
      {
        "userId": 1,
        "role": "parent",
        "displayName": "엄마",
        "ageBand": "60s",
        "gender": "female",
        "relationLabel": "엄마",
        "profileImage": "coral",
        "profileCompleted": true,
        "personalityResult": {
          "code": "healing_traveler",
          "name": "풍경 수집가",
          "catchphrase": "쉬엄쉬엄이 최고!",
          "description": "천천히 걷고 오래 머무는 여행을 좋아합니다."
        }
      }
    ]
  }
}

```

---

## POST /api/v1/family/code

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "가족 코드 조회 성공",
  "data": {
    "code": "MSH-2405"
  }
}
```

---

## POST /api/v1/family/matches

심사 기간에 `APP_REVIEW_FAMILY_CODE`가 설정되어 있으면 그 코드는 일반 코드 조회를 건너뛰고 심사용 데모 가족을 새로 만들어 연결합니다. 응답 형식은 일반 매칭과 같습니다. 자세한 동작과 운영 절차는 `docs/policy/app-review-family-code.md`를 따릅니다.

### Request

```json
{
  "code": "MSH0001"
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `code` | string | true | 상대방 가족 코드. `MSH0001`, `MSH-0001` 모두 허용 |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "가족 매칭 성공",
  "data": {
    "familyId": 1,
    "matchedUser": {
      "id": 2,
      "role": "child",
      "displayName": "혜린"
    },
    "members": [
      {
        "userId": 2,
        "role": "child",
        "displayName": "혜린",
        "ageBand": "20s",
        "gender": "female",
        "relationLabel": null,
        "profileImage": "green",
        "profileCompleted": false,
        "personalityResult": null
      },
      {
        "userId": 1,
        "role": "parent",
        "displayName": "엄마",
        "ageBand": "60s",
        "gender": "female",
        "relationLabel": "엄마",
        "profileImage": "coral",
        "profileCompleted": true,
        "personalityResult": {
          "code": "healing_traveler",
          "name": "풍경 수집가",
          "catchphrase": "쉬엄쉬엄이 최고!",
          "description": "천천히 걷고 오래 머무는 여행을 좋아합니다."
        }
      }
    ]
  }
}
```

---

## 주요 실패 조건

| 본문 status | 조건 |
|--------|------|
| `400` | 내 코드를 입력한 경우 |
| `400` | 같은 역할끼리 매칭하는 경우 |
| `400` | 이미 다른 가족과 연결된 사용자인 경우 |
| `400` | 가족에 부모가 이미 2명 연결된 경우 |
| `401` | access token이 없거나 유효하지 않은 경우 |
| `404` | 가족 코드를 찾을 수 없는 경우 |

---

## DELETE /api/v1/dev/family

**개발 편의용 API입니다. 앱에서 호출하지 않습니다.** 가족 연결을 다시 테스트할 수 있도록 내 가족 연결을 끊습니다.

별도 스위치 없이 항상 열려 있습니다. 인증만 있으면 누구나 호출할 수 있고 되돌릴 수 없으니 앱에서는 노출하지 않습니다.

| 호출자 | 동작 |
|--------|------|
| 자녀 | 가족을 해체합니다. 구성원 전원의 연결을 끊고, 전원의 가족 코드를 비활성화하고, 그 가족의 여행을 모두 soft delete하고, 가족을 비활성화합니다 |
| 부모 | 본인 연결과 본인 가족 코드만 정리합니다. 가족과 여행은 그대로 둡니다 |

자녀가 호출할 때 여행까지 지우는 이유는 두 가지입니다. 자녀 없는 가족은 여행을 만들 수 없어 남길 이유가 없고, 여행을 남기면 참여 기준으로 조회하는 기록 탭에 계속 보여서 초기화가 반쪽이 됩니다.

부모 프로필과 여행 MBTI 결과는 사용자에 붙은 값이라 지우지 않습니다. 다시 연결하면 그대로 쓸 수 있습니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "가족 연결 해제 성공",
  "data": {
    "familyId": 1,
    "dissolved": true,
    "removedUserIds": [1, 2, 3],
    "deletedTripIds": [10, 11]
  }
}
```

| Field | Type | 설명 |
|-------|------|------|
| `familyId` | number | 해제한 가족 ID |
| `dissolved` | boolean | 가족을 통째로 해체했는지 여부. 자녀가 호출하면 `true` |
| `removedUserIds` | array | 연결이 끊긴 사용자 ID |
| `deletedTripIds` | array | 함께 삭제한 여행 ID. 부모가 호출하면 빈 배열 |

연결된 가족이 없으면 `status=400`, `message=연결된 가족이 없습니다.`를 반환합니다.
