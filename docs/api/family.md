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
          "name": "유유자적 힐링러형",
          "catchphrase": "여행은 쉬러 가는 거지.",
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
          "name": "유유자적 힐링러형",
          "catchphrase": "여행은 쉬러 가는 거지.",
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
