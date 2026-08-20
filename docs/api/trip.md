# Trip API

여행 생성 기본 흐름을 처리합니다.

모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

> 응답 규칙: 서버가 처리한 요청·Validation·인증·정책 오류도 HTTP `200`으로 반환하며, 문서의 `400/401/403/404`는 본문 `status`입니다. 외부 API·서버 실패는 실제 HTTP `500`입니다.

---

## 결정 사항

- 여행 생성은 현재 자녀 사용자만 가능합니다.
- 여행 대상은 같은 가족에 연결된 부모만 선택할 수 있습니다.
- 선택한 부모의 상세 프로필이 `completed` 상태여야 여행을 생성할 수 있습니다.
- 부모 후보 응답은 프로필 작성 단계와 현재 여행 MBTI 결과 표시 정보를 함께 내려줍니다. 결과 유형명은 부모 프로필 정책의 기존 명칭을 그대로 사용합니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 같은 가족에서 날짜가 겹치는 여행은 생성할 수 없습니다.
- 도시 목록은 현재 서버 고정 catalog로 내려주고, 여행에는 `destinationCode` 문자열을 저장합니다.
- 여행 제목은 필수이며 공백을 제외한 15자 이하로 입력합니다.
- 날짜 선택 화면은 별도 캘린더 API 없이 `GET /api/v1/trips`의 비보관 여행 기간을 사용합니다. 날짜 중복은 여행 생성 시 서버에서도 다시 검증합니다.
- 여행 생성 시 선택한 부모의 추천 입력값을 `recommendationSnapshot`으로 저장합니다. 이후 부모 프로필이 수정되어도 생성 당시 추천 기준은 유지됩니다.
- 여행 기본정보 수정은 여행을 만든 자녀만 할 수 있습니다. 제목, 기간, 참여 부모를 변경할 수 있으며 도시는 생성 이후 고정합니다.
- 날짜나 참여 부모를 수정하면 현재 부모 프로필로 추천 스냅샷을 다시 만들되, 기존 코스와 경로는 일차 번호 기준으로 유지합니다.
- 참여 부모 구성이 바뀌면 저장된 여행 10계명, 문구 10개, 모든 참여자 서명을 함께 삭제하고 처음부터 다시 작성하도록 합니다.
- 여행 기간 또는 참여 부모 구성이 바뀌면 기존 부모 평가 요청, 제출된 피드백, 효도 리포트도 함께 삭제합니다.
- 여행을 만든 자녀는 여행 상태와 관계없이 soft delete할 수 있습니다.
- 여행을 만든 자녀는 `in_progress` 여행을 수동 종료할 수 있습니다. 정상 종료와 동일하게 `completed` 상태로 기록에 남고 부모 평가를 바로 진행할 수 있습니다.
- 코스 전체 저장, 추천 재생성, 경로 최적화는 여행을 만든 자녀만 `planning`, `ready`, `in_progress` 상태에서 할 수 있습니다. `completed`, `stopped`, `archived` 여행은 변경할 수 없습니다.
- 여행 코스는 일자별 방문지 목록을 전체 저장합니다. 요청 배열 순서가 방문 순서가 되며, 포함하지 않은 일자는 빈 코스로 저장됩니다.
- 저장된 방문지는 외부 API 원본이 바뀌어도 기존 코스를 유지할 수 있도록 `trip_stops`에 장소 스냅샷을 저장합니다.
- 추천 코스 생성은 한국관광공사 TourAPI 무장애여행 서비스로 장소 후보를 조회하고, 부모 프로필 스냅샷으로 점수를 계산해 `trip_stops`에 저장합니다.
- 추천 코스 생성은 장소 추천까지만 수행합니다. Tmap 경로/거리/소요시간은 별도 경로 최적화 API를 호출해 계산합니다.
- 추천 코스 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`로 변경됩니다.
- 코스 편집 중 부모 프로필 기반 추천, 키워드 검색, 상세 조회는 TourAPI를 사용하며, 실제 코스 반영은 클라이언트가 선택한 장소를 `PUT /api/v1/trips/{tripId}/course`로 전체 저장할 때 이루어집니다.
- 방문지 메모는 코스 전체 저장과 별개로 단건 저장하거나 삭제합니다.
- 방문지 검색/상세 조회에서도 숙박은 제외하고, TourAPI 원본 응답 일부는 코스 저장 시 `sourcePayload`로 보관할 수 있게 내려줍니다.
- 일자별 경로 최적화는 Tmap 경유지 순서 최적화 10 API를 사용합니다. 사용자가 시작점/도착점을 입력하지 않으므로 서버가 모든 시작/도착 조합을 조회해 가장 짧은 결과를 선택합니다.
- 코스 전체 저장은 방문지 구성이 달라진 일자의 경로 캐시만 무효화합니다. 추천 코스 재생성은 모든 일자의 경로 캐시를 무효화합니다.
- 서울 날짜 기준 시작일이 되면 준비 여부와 관계없이 여행 상태를 `in_progress`, 종료일 다음 날부터 `completed`로 자동 동기화합니다.
- 여행 모드는 시작일 00:00부터 종료일 23:59까지 같은 가족 구성원 모두가 이용할 수 있습니다. 여행 참여자로 선택되지 않은 가족 구성원도 포함합니다.
- 여행 모드 주변 공중화장실은 DB 적재 데이터를 조회하고, 병원·약국은 Tmap POI 주변 카테고리 검색 결과를 실시간으로 조회합니다.
- 여행 모드 주변 카페는 Tmap POI 주변 카테고리 검색 결과를 실시간으로 조회합니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/api/v1/trips/parent-candidates` | 여행 대상 부모 후보 조회 |
| `GET` | `/api/v1/trips/destinations` | 여행 도시 목록 조회 |
| `GET` | `/api/v1/trips` | 내 가족 여행 목록 조회 |
| `GET` | `/api/v1/trips/{tripId}` | 여행 상세 조회 |
| `GET` | `/api/v1/trips/{tripId}/course` | 여행 코스 조회 |
| `GET` | `/api/v1/trips/{tripId}/travel-mode` | 현재 일차와 전체 코스를 포함한 여행 모드 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-cafes` | 현재 위치 주변 카페 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-restrooms` | 현재 위치 주변 공중화장실 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-hospitals` | 현재 위치 주변 병원 조회 |
| `GET` | `/api/v1/trips/{tripId}/nearby-pharmacies` | 현재 위치 주변 약국 조회 |
| `GET` | `/api/v1/trips/{tripId}/places/recommendations` | 코스 편집용 부모 프로필 기반 방문지 추천 |
| `GET` | `/api/v1/trips/{tripId}/places/search` | 코스 편집용 방문지 검색 |
| `GET` | `/api/v1/trips/{tripId}/places/{contentId}` | 코스 편집용 방문지 상세 조회 |
| `POST` | `/api/v1/trips` | 여행 생성 |
| `POST` | `/api/v1/trips/{tripId}/stop` | 진행 중 여행 중단 |
| `POST` | `/api/v1/trips/{tripId}/course/recommendation` | 여행 추천 코스 생성 |
| `POST` | `/api/v1/trips/{tripId}/days/{dayNumber}/route-optimization` | 여행 일자 경로 최적화 |
| `PUT` | `/api/v1/trips/{tripId}` | 여행 기본정보 수정 |
| `PUT` | `/api/v1/trips/{tripId}/course` | 여행 코스 전체 저장 |
| `PUT` | `/api/v1/trips/{tripId}/stops/{stopId}/note` | 방문지 메모 저장·삭제 |
| `DELETE` | `/api/v1/trips/{tripId}` | 여행 soft delete |

---

## GET /api/v1/trips/parent-candidates

자녀 사용자가 같은 가족에 연결된 부모와 부모 상세 프로필 완료 여부를 조회합니다.
가족 매칭 전이면 `familyId=null`, `parents=[]`를 반환합니다.
`profileExists=false`이면 프로필 미입력, `profileExists=true`이면서 `profileCompleted=false`이면 `profileCurrentStep`과 `profileCompletionPercent`로 작성 중 상태를 표시합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 대상 부모 후보 조회 성공",
  "data": {
    "familyId": 1,
    "parents": [
      {
        "userId": 2,
        "displayName": "김영희",
        "gender": "female",
        "relationLabel": "엄마",
        "profileExists": true,
        "profileCompleted": true,
        "profileStatus": "completed",
        "profileCompletionPercent": 100,
        "profileCurrentStep": 3,
        "personalityResult": {
          "code": "healing_traveler",
          "name": "유유자적 힐링러형",
          "catchphrase": "여행은 쉬러 가는 거지.",
          "description": "자연풍경, 역사, 산책을 좋아하며 천천히 둘러보는 타입이시네요. 음식도 한식처럼 편안한 선택을 선호하시는 편이에요."
        }
      }
    ]
  }
}
```

---

## GET /api/v1/trips/destinations

도시 선택 화면에 표시할 서버 고정 여행 도시 목록을 조회합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 도시 목록 조회 성공",
  "data": [
    {
      "code": "busan",
      "displayName": "부산",
      "displayOrder": 4,
      "badgeLabel": "Hot!"
    }
  ]
}
```

| 순서 | `code` | 표시 이름 | 배지 |
|------|--------|-----------|------|
| 1 | `gangneung_sokcho` | 강릉·속초 | - |
| 2 | `gyeongju` | 경주 | - |
| 3 | `daegu` | 대구 | - |
| 4 | `busan` | 부산 | `Hot!` |
| 5 | `seoul` | 서울 | - |
| 6 | `suwon_yongin` | 수원·용인 | - |
| 7 | `yeosu` | 여수 | - |
| 8 | `incheon` | 인천 | - |
| 9 | `jeonju` | 전주 | - |
| 10 | `jeju` | 제주 | - |
| 11 | `tongyeong_geoje_namhae` | 통영·거제·남해 | - |
| 12 | `pohang_andong` | 포항·안동 | - |

---

## POST /api/v1/trips

자녀 사용자가 여행 대상 부모, 도시, 날짜와 제목을 선택해 여행 기본 정보를 생성합니다.

### Request

```json
{
  "parentUserIds": [2],
  "destinationCode": "gyeongju",
  "startDate": "2026-07-10",
  "endDate": "2026-07-11",
  "title": "아빠와 단둘이 경주"
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `parentUserIds` | number array | true | 같은 가족에 연결된 부모 사용자 ID 목록. 최대 2명 |
| `destinationCode` | enum | true | 여행 도시 코드 |
| `startDate` | date | true | 여행 시작일 |
| `endDate` | date | true | 여행 종료일. 시작일과 같거나 이후 |
| `title` | string | true | 공백을 제외하고 필수. 최대 15자 |

### Response

```json
{
  "status": 201,
  "success": true,
  "message": "여행 생성 성공",
  "data": {
    "id": 1,
    "familyId": 1,
    "title": "아빠와 단둘이 경주",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "startDate": "2026-07-10",
    "endDate": "2026-07-11",
    "status": "planning",
    "participants": [
      {
        "userId": 1,
        "role": "child",
        "displayName": "최혜린",
        "gender": "female",
        "relationLabel": null
      },
      {
        "userId": 2,
        "role": "parent",
        "displayName": "김영희",
        "gender": "female",
        "relationLabel": "엄마"
      }
    ],
    "recommendationSnapshot": {
      "policyVersion": "parent-travel-mbti-v1",
      "capturedAt": "2026-07-06T12:00:00",
      "destinationCode": "gyeongju",
      "startDate": "2026-07-10",
      "endDate": "2026-07-11",
      "parents": [
        {
          "parentUserId": 2,
          "parentProfileId": 1,
          "displayName": "김영희",
          "relationLabel": "엄마",
          "walkingPace": "slow",
          "needsMobilityAssistance": false,
          "travelThemes": ["nature_scenery"],
          "foodPreference": "familiar",
          "personalityType": "healing_traveler",
          "profileCompletedAt": "2026-07-01T12:00:00"
        }
      ]
    },
    "days": [
      {
        "id": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10"
      },
      {
        "id": 2,
        "dayNumber": 2,
        "travelDate": "2026-07-11"
      }
    ]
  }
}
```

미래 여행은 생성 직후 `status=planning`입니다. 시작일이 오늘이면 생성 응답부터 `in_progress`이며, 추천 코스 생성이 완료된 미래 여행은 `ready`가 됩니다.
서울 날짜 기준 시작일부터 `in_progress`, 종료일 다음 날부터 `completed`로 자동 전환합니다.
`recommendationSnapshot.policyVersion`은 부모 여행 MBTI 정책 버전을 의미합니다.

### 클라이언트 생성 플로우

확정 화면은 별도 비동기 job이나 진행률 조회 API 없이 아래 API를 순서대로 호출합니다.

1. `GET /api/v1/trips/parent-candidates`로 선택 가능한 부모와 프로필 상태를 조회합니다.
2. `GET /api/v1/trips/destinations`로 서버가 정한 도시 순서와 배지를 표시합니다.
3. `GET /api/v1/trips`에서 `status`가 `stopped`, `archived`가 아닌 여행 기간을 캘린더의 `다른 여행` 범위로 표시합니다.
4. 제목과 날짜를 확정하면 `POST /api/v1/trips`로 여행과 일자를 생성합니다.
5. 반환된 `tripId`로 `POST /api/v1/trips/{tripId}/course/recommendation`을 호출해 모든 일자의 장소를 생성합니다.
6. 추천 응답의 각 `dayNumber`에 대해 `POST /api/v1/trips/{tripId}/days/{dayNumber}/route-optimization`을 호출합니다.
7. 모든 일자의 경로 최적화가 성공하면 `홈으로 가기`를 활성화합니다. 실패한 호출은 같은 `tripId`, `dayNumber`로 재시도할 수 있습니다.

`코스 생성 중` 화면의 단계와 퍼센트는 위 동기 호출 상태를 클라이언트가 표현하는 값입니다. 서버는 별도 생성 job, 진행률, 화면 문구를 저장하지 않습니다.

---

## PUT /api/v1/trips/{tripId}

여행을 만든 자녀가 여행 정보 편집 화면의 제목, 날짜, 참여 부모를 한 번에 저장합니다.
도시는 생성 후 변경할 수 없으므로 요청에 포함하지 않습니다. `in_progress`에서는 서울 기준 오늘을 포함하는 기간만 선택할 수 있습니다.
`completed`, `stopped`, `archived` 상태에서는 수정할 수 없습니다.

### Request

```json
{
  "title": "부모님과 경주 여행",
  "startDate": "2026-07-15",
  "endDate": "2026-07-17",
  "parentUserIds": [2, 3]
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `title` | string | true | 공백을 제외하고 필수. 최대 15자. 제목만 변경하면 코스 등 다른 데이터 유지 |
| `startDate` | date | true | 여행 시작일. 여행 중에는 변경 기간에 오늘이 포함되어야 함 |
| `endDate` | date | true | 여행 종료일. 시작일과 같거나 이후이며 여행 중에는 변경 기간에 오늘이 포함되어야 함 |
| `parentUserIds` | number array | true | 같은 가족의 프로필 작성 완료 부모. 1명 이상, 최대 2명. 기존 참여 부모 구성이 바뀌면 10계명과 모든 서명 삭제 |

변경 영향:

| 변경 항목 | 기존 코스 | 추천 스냅샷 | 10계명/서명 | 평가 요청/피드백/리포트 | 상태 |
|-----------|-----------|-----------------|-------------|--------------------------|------|
| 제목 변경 | 전체 유지 | 유지 | 유지 | 유지 | 유지 |
| 날짜 변경 | 일차 번호 기준 유지. 줄어든 뒤쪽 일차만 삭제하고 늘어난 일차는 빈 일정 추가 | 현재 부모 프로필로 재생성 | 유지 | 전체 삭제 | 준비 중이면 `planning`, 여행 중이면 `in_progress` |
| 참여 부모 변경 | 전체 유지 | 현재 부모 프로필로 재생성 | 전체 삭제 | 전체 삭제 | 준비 중이면 `planning`, 여행 중이면 `in_progress` |

다른 여행과 날짜가 겹치거나, 같은 가족의 작성 완료 부모가 아니면 수정할 수 없습니다.
참여 부모 ID 집합이 실제로 바뀌어 수정이 성공하면 별도 확인값 없이 기존 10계명과 모든 서명이 삭제되며, 자녀부터 다시 작성하고 서명해야 합니다.
날짜 또는 참여 부모가 바뀌면 기존 평가 요청, 부모 피드백, 효도 리포트도 삭제되므로 변경된 조건에서 다시 요청하고 제출해야 합니다.

### Response

`GET /api/v1/trips/{tripId}`와 같은 `TripDetailResponse` 형태입니다.

상세 정책은 `docs/policy/trip-edit.md`를 따릅니다.

---

## DELETE /api/v1/trips/{tripId}

여행을 만든 자녀가 상태와 관계없이 여행을 삭제합니다.

- 실제 row는 지우지 않고 `trips.deleted_at`을 기록합니다.
- 삭제된 여행은 목록·상세·코스·기록 조회에서 제외됩니다.
- 삭제된 여행 기간은 날짜 중복 검사에서 제외되므로 같은 날짜로 새 여행을 만들 수 있습니다.
- 성공 응답은 `data` 없이 `status=200`, `message=여행 삭제 성공`입니다.

---

## POST /api/v1/trips/{tripId}/stop

여행을 만든 자녀가 현재 `in_progress`인 여행을 수동 종료합니다. Request Body는 없습니다.

- 상태를 `completed`로 변경하고 기존 참여자·일자·코스와 평가 요청·피드백·효도 리포트 데이터를 유지합니다.
- 종료 후 여행 모드와 기본정보·코스 편집은 사용할 수 없습니다.
- 날짜로 정상 종료된 여행과 동일하게 `GET /api/v1/records`, 완료 여행 통계, 평가, 효도 리포트 대상에 포함합니다.
- 계획 종료일 전이라도 수동 종료 직후 부모 평가 요청과 제출을 진행할 수 있습니다.
- 성공 응답은 상태가 `completed`인 `TripDetailResponse`입니다.

---

## GET /api/v1/trips

로그인 사용자가 속한 가족의 여행 목록을 조회합니다.
가족 매칭 전이면 빈 목록을 반환합니다.
조회 시 서울 날짜를 기준으로 시작한 여행은 `in_progress`, 종료된 여행은 `completed`로 상태를 동기화합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "내 가족 여행 목록 조회 성공",
  "data": {
    "familyId": 1,
    "trips": [
      {
        "id": 1,
        "title": "경주 여행",
        "destination": {
          "code": "gyeongju",
          "displayName": "경주",
          "displayOrder": 2,
          "badgeLabel": null
        },
        "startDate": "2026-07-10",
        "endDate": "2026-07-11",
        "status": "planning",
        "participantCount": 2
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}

현재 같은 가족 구성원이 여행 상세를 조회합니다. 가족 연결이 해제된 뒤에는 해당 여행의 기존 참여자만 `completed`, `stopped` 여행 상세를 계속 조회할 수 있습니다.
다른 가족 여행이면 HTTP `200`, 본문 `status=403`을 반환합니다.
응답 전 여행 상태를 현재 서울 날짜에 맞춰 동기화합니다.

### Response

`POST /api/v1/trips`와 같은 `TripDetailResponse` 형태입니다.

---

## GET /api/v1/trips/{tripId}/course

현재 같은 가족 구성원이 여행 코스를 조회합니다. 가족 연결이 해제된 뒤에는 해당 여행의 기존 참여자만 `completed` 여행 코스를 계속 조회할 수 있습니다.
저장된 방문지가 없으면 여행 일자별로 `stops=[]`를 반환합니다.
응답 전 여행 상태를 현재 서울 날짜에 맞춰 동기화합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 코스 조회 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "startDate": "2026-07-10",
    "endDate": "2026-07-11",
    "status": "planning",
    "participants": [
      {
        "userId": 1,
        "role": "child",
        "displayName": "최혜린",
        "gender": "female",
        "relationLabel": null
      },
      {
        "userId": 2,
        "role": "parent",
        "displayName": "김영희",
        "gender": "female",
        "relationLabel": "엄마"
      }
    ],
    "pledgeStatus": "awaiting_parent_signature",
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "route": {
          "provider": "tmap",
          "totalDistanceMeters": 6230,
          "totalDurationSeconds": 1757,
          "optimizedAt": "2026-07-07T12:00:00",
          "polyline": [
            {
              "longitude": 129.2247,
              "latitude": 35.8562
            }
          ],
          "sourcePayload": {
            "provider": "tmap",
            "operation": "routeOptimization10",
            "policyVersion": "tmap-route-optimization-v1"
          }
        },
        "stops": [
          {
            "id": 1,
            "sortOrder": 1,
            "stopType": "sightseeing",
            "sourceProvider": "tour_api",
            "externalPlaceId": "988449",
            "contentTypeId": "12",
            "name": "오도리 공원",
            "category": "관광지",
            "address": "대구광역시 동구 효목동",
            "latitude": 35.8821234,
            "longitude": 128.6212345,
            "phone": "053-123-4567",
            "homepageUrl": "https://example.com",
            "imageUrl": "https://example.com/park.jpg",
            "overview": "짧은 산책을 즐기기 좋은 공원입니다.",
            "arrivalTime": "10:30:00",
            "dwellMinutes": 60,
            "note": "부모님과 사진 찍기",
            "recommendationReason": "짧은 산책과 휴식에 적합합니다.",
            "recommendationTags": ["nature_scenery", "low_slope"],
            "sourcePayload": {
              "contentid": "988449",
              "route": "출입구까지 경사로가 설치되어 있음"
            },
            "isManualAdded": false
          }
        ]
      }
    ]
  }
}
```

`pledgeStatus`는 코스 화면의 10계명 배너를 결정합니다.

| 값 | 화면 상태 |
|----|-----------|
| `not_created` | 10계명 만들기 |
| `awaiting_child_signature` | 자녀 서명 필요 |
| `awaiting_parent_signature` | 부모 서명 필요 |
| `completed` | 서명 완료 |

---

## GET /api/v1/trips/{tripId}/travel-mode

같은 가족 구성원이 여행 기간 중 현재 일차와 전체 일자별 코스를 조회합니다.
여행 참여자로 선택되지 않은 같은 가족 구성원도 접근할 수 있습니다.

- 시작일 전: HTTP `200`, 본문 `status=400`
- 시작일 00:00부터 종료일 23:59까지: HTTP `200`, 본문 `status=200`, 여행 상태 `in_progress`
- 종료일 다음 날부터: 여행 상태를 `completed`로 동기화한 뒤 HTTP `200`, 본문 `status=400`
- 다른 가족 사용자: HTTP `200`, 본문 `status=403`

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 모드 조회 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "startDate": "2026-07-10",
    "endDate": "2026-07-11",
    "status": "in_progress",
    "currentDayNumber": 1,
    "currentTripDayId": 1,
    "isLastDay": false,
    "participants": [
      {
        "userId": 1,
        "displayName": "혜린",
        "role": "child",
        "gender": "female",
        "relationLabel": null
      },
      {
        "userId": 2,
        "displayName": "김지영",
        "role": "parent",
        "gender": "female",
        "relationLabel": "엄마"
      }
    ],
    "pledgeCompleted": true,
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "route": null,
        "stops": []
      }
    ]
  }
}
```

`pledgeCompleted=true`는 자녀와 참여 부모 최소 1명의 서명이 완료되었다는 뜻입니다.
상세 정책은 `docs/policy/travel-mode.md`를 따릅니다.

---

## POST /api/v1/trips/{tripId}/course/recommendation

여행을 만든 자녀가 여행 추천 코스를 생성합니다.
요청 본문은 받지 않습니다. 서버는 여행 생성 시 저장한 `recommendationSnapshot`, 여행 도시, 여행 일자를 기준으로 TourAPI 장소 후보를 조회합니다.
`planning`, `ready`, `in_progress` 상태에서 호출할 수 있고 `completed`, `stopped`, `archived` 상태에서는 호출할 수 없습니다.

생성 규칙:

- 숙박을 제외한 TourAPI 콘텐츠 타입을 후보로 사용합니다.
- 부모 프로필의 `walkingPace` 기준으로 하루 장소 수를 정합니다.
- 부모가 2명인 경우 더 천천히 걷는 부모 기준을 사용합니다.
- 음식점 후보가 있으면 각 일자에 식사 장소 1곳을 포함합니다.
- 이동 도움이 필요한 부모가 있으면 TourAPI 무장애 정보 문자열 존재 여부를 점수에 반영합니다.
- 기존 코스가 있으면 추천 결과로 덮어씁니다.
- 추천 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`가 됩니다.

추천 정책 상세는 `docs/policy/course-recommendation.md`를 따릅니다.

### Request

없음.

### Response

`GET /api/v1/trips/{tripId}/course`와 같은 `TripCourseResponse` 형태입니다.

```json
{
  "status": 200,
  "success": true,
  "message": "여행 추천 코스 생성 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "status": "ready",
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "stops": [
          {
            "id": 1,
            "sortOrder": 1,
            "stopType": "sightseeing",
            "sourceProvider": "tour_api",
            "externalPlaceId": "988449",
            "contentTypeId": "12",
            "name": "오도리 공원",
            "category": "관광지",
            "recommendationReason": "부모님 선호 테마와 무장애 정보를 함께 반영한 추천 장소입니다.",
            "recommendationTags": ["tour_api", "type:12", "na", "mobility_info"],
            "sourcePayload": {
              "provider": "tour_api",
              "recommendation": {
                "policyVersion": "tour-api-course-recommendation-v1",
                "score": 51
              }
            },
            "isManualAdded": false
          }
        ]
      }
    ]
  }
}
```

TourAPI 서비스키가 서버에 설정되어 있지 않거나 TourAPI 호출이 실패하면 `500`을 반환합니다.

---

## POST /api/v1/trips/{tripId}/days/{dayNumber}/route-optimization

여행을 만든 자녀가 특정 일자의 방문지 순서를 Tmap 기준으로 최적화합니다.
사용자가 시작점/도착점을 입력하지 않으므로 서버가 모든 시작/도착 조합을 탐색한 뒤 가장 짧은 결과를 선택합니다.
`planning`, `ready`, `in_progress` 상태에서 호출할 수 있고 `completed`, `stopped`, `archived` 상태에서는 호출할 수 없습니다.

최적화 결과는 아래에 반영됩니다.

- `trip_stops.sort_order`
- `trip_stops.arrival_time`
- 비어 있던 `trip_stops.dwell_minutes` 기본값
- `trip_days`의 일자별 route 캐시

제약:

- 하루 방문지 3곳 이상, 10곳 이하만 처리합니다.
- 모든 방문지에 `latitude`, `longitude`가 있어야 합니다.
- Tmap 앱키가 서버에 설정되어 있어야 합니다.

정책 상세는 `docs/policy/route-optimization.md`를 따릅니다.

### Request

없음.

### Response

`GET /api/v1/trips/{tripId}/course`와 같은 `TripCourseResponse` 형태입니다.

```json
{
  "status": 200,
  "success": true,
  "message": "여행 일자 경로 최적화 성공",
  "data": {
    "tripId": 1,
    "title": "경주 여행",
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "status": "ready",
    "days": [
      {
        "tripDayId": 1,
        "dayNumber": 1,
        "travelDate": "2026-07-10",
        "route": {
          "provider": "tmap",
          "totalDistanceMeters": 6230,
          "totalDurationSeconds": 1757,
          "optimizedAt": "2026-07-07T12:00:00",
          "polyline": [
            {
              "longitude": 129.2247,
              "latitude": 35.8562
            }
          ],
          "sourcePayload": {
            "provider": "tmap",
            "operation": "routeOptimization10",
            "policyVersion": "tmap-route-optimization-v1",
            "orderedStopIds": [2, 3, 1]
          }
        },
        "stops": [
          {
            "id": 2,
            "sortOrder": 1,
            "name": "경주 한식당",
            "arrivalTime": "10:00:00",
            "dwellMinutes": 60
          }
        ]
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}/places/recommendations

코스 편집의 기본 화면에서 여행 도시와 여행 생성 당시 부모 프로필 스냅샷을 기준으로 추천 장소를 조회합니다.
현재 코스에 이미 저장된 TourAPI `contentId`는 제외하며 DB에는 추천 조회 결과를 저장하지 않습니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `category` | enum | true | `restaurant` 또는 `attraction` |
| `size` | number | false | 추천 개수. 기본값 `20`, 최대 `50` |

- `restaurant`: 음식점(`contentTypeId=39`)
- `attraction`: 관광지, 문화시설, 행사, 레포츠, 쇼핑(`12`, `14`, `15`, `28`, `38`)

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "방문지 추천 성공",
  "data": {
    "tripId": 1,
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "category": "restaurant",
    "places": [
      {
        "sourceProvider": "tour_api",
        "externalPlaceId": "988449",
        "contentTypeId": "39",
        "contentTypeName": "음식점",
        "stopType": "meal",
        "name": "경주 한식당",
        "category": "한식",
        "categoryCode": "A05020100",
        "address": "경상북도 경주시",
        "latitude": 35.8562,
        "longitude": 129.2247,
        "phone": "054-000-0000",
        "imageUrl": "https://example.com/place.jpg",
        "lclsSystm1": "FD"
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}/places/search

코스 편집 화면에서 여행 도시 범위 안의 TourAPI 방문지를 키워드로 검색합니다.
검색 결과는 추천 후보 목록으로만 사용하며, 사용자가 선택한 장소를 코스에 반영하려면 `PUT /api/v1/trips/{tripId}/course`로 저장해야 합니다.

숙박은 검색 결과에서 제외합니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `keyword` | string | true | 검색어. 공백 제거 후 1자 이상, 50자 이하 |
| `category` | enum | true | `restaurant` 또는 `attraction` |
| `page` | number | false | 페이지 번호. 기본값 `1` |
| `size` | number | false | 페이지 크기. 기본값 `20`, 최대 `50` |

`category`별 콘텐츠 타입은 방문지 추천 API와 같습니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "방문지 검색 성공",
  "data": {
    "tripId": 1,
    "destination": {
      "code": "gyeongju",
      "displayName": "경주",
      "displayOrder": 2,
      "badgeLabel": null
    },
    "keyword": "경주 맛집",
    "category": "restaurant",
    "page": 1,
    "size": 20,
    "places": [
      {
        "sourceProvider": "tour_api",
        "externalPlaceId": "988449",
        "contentTypeId": "39",
        "contentTypeName": "음식점",
        "stopType": "meal",
        "name": "경주 한식당",
        "category": "한식",
        "categoryCode": "A05020100",
        "address": "경상북도 경주시",
        "latitude": 35.8562,
        "longitude": 129.2247,
        "phone": "054-000-0000",
        "imageUrl": "https://example.com/place.jpg",
        "lclsSystm1": "FD"
      }
    ]
  }
}
```

---

## GET /api/v1/trips/{tripId}/places/{contentId}

코스 편집 화면에서 TourAPI 방문지 상세와 무장애 주요 정보를 조회합니다.
장소 상세 화면 표시와, 선택 장소를 코스 저장 API에 넣기 전 스냅샷 데이터를 구성하는 데 사용합니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `contentTypeId` | string | false | 검색 목록에서 알고 있는 TourAPI 콘텐츠 타입 ID. 상세 응답에 타입이 없을 때 보조값으로 사용 |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "방문지 상세 조회 성공",
  "data": {
    "sourceProvider": "tour_api",
    "externalPlaceId": "988449",
    "contentTypeId": "12",
    "contentTypeName": "관광지",
    "stopType": "sightseeing",
    "name": "오도리 공원",
    "category": "관광지",
    "categoryCode": "NA010100",
    "address": "경상북도 경주시",
    "latitude": 35.8562,
    "longitude": 129.2247,
    "phone": "054-000-0000",
    "homepageUrl": "https://example.com",
    "imageUrl": "https://example.com/place.jpg",
    "imageUrls": [
      "https://example.com/place.jpg",
      "https://example.com/place-sub.jpg"
    ],
    "overview": "산책하기 좋은 공원입니다.",
    "operatingHours": "09:00~18:00",
    "closedDays": "매주 월요일",
    "admissionFee": "무료",
    "lclsSystm1": "NA",
    "accessibility": {
      "parking": "장애인 주차장 있음",
      "publicTransport": null,
      "route": "출입구까지 경사로 있음",
      "wheelchair": null,
      "exit": "휠체어 접근 가능",
      "elevator": null,
      "restroom": "장애인 화장실 있음"
    },
    "recommendationTags": ["tour_api", "type:12", "na", "mobility_info"],
    "sourcePayload": {
      "provider": "tour_api",
      "detailCommon": {
        "contentid": "988449"
      },
      "detailIntro": {
        "usetime": "09:00~18:00"
      },
      "detailImages": [
        {
          "originimgurl": "https://example.com/place-sub.jpg"
        }
      ],
      "accessibility": {
        "route": "출입구까지 경사로 있음"
      }
    }
  }
}
```

---

## PUT /api/v1/trips/{tripId}/course

여행을 만든 자녀가 일자별 방문지 코스를 전체 저장합니다.
요청 배열 순서가 해당 일자의 방문 순서가 됩니다.
기존 코스는 저장 요청 기준으로 덮어쓰며, 요청에 포함하지 않은 일자는 빈 코스로 저장됩니다.
코스를 저장하면 방문지 구성이 달라진 일자의 Tmap 경로 최적화 캐시만 무효화됩니다.
`planning`, `ready`, `in_progress` 상태에서 저장할 수 있고 `completed`, `stopped`, `archived` 상태에서는 저장할 수 없습니다.

> 이 엔드포인트가 방문지 추가·수정·삭제·순서 변경을 모두 처리합니다. 단건 편집 API가 아닌 전체 덮어쓰기이므로 클라이언트는 화면에서 임시 편집한 뒤, 변경하지 않은 일자를 포함한 최종 코스 전체를 `저장하기` 시점에 전송해야 합니다.
> 저장 성공 후 응답에서 `route`가 `null`인 변경 일자마다 `POST /api/v1/trips/{tripId}/days/{dayNumber}/route-optimization`을 호출한 뒤 코스 화면으로 돌아갑니다. 방문지 구성이 같은 일자의 방문지 ID와 기존 경로는 유지됩니다.

### Request

```json
{
  "days": [
    {
      "dayNumber": 1,
      "stops": [
        {
          "stopType": "sightseeing",
          "sourceProvider": "tour_api",
          "externalPlaceId": "988449",
          "contentTypeId": "12",
          "name": "오도리 공원",
          "category": "관광지",
          "address": "대구광역시 동구 효목동",
          "latitude": 35.8821234,
          "longitude": 128.6212345,
          "arrivalTime": "10:30",
          "dwellMinutes": 60,
          "note": "부모님과 사진 찍기",
          "recommendationReason": "짧은 산책과 휴식에 적합합니다.",
          "recommendationTags": ["nature_scenery", "low_slope"],
          "sourcePayload": {
            "contentid": "988449",
            "route": "출입구까지 경사로가 설치되어 있음"
          },
          "isManualAdded": false
        }
      ]
    }
  ]
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `days[].dayNumber` | number | true | 여행 며칠차. 해당 여행에 존재하는 일자만 가능 |
| `days[].stops[]` | array | false | 해당 일자의 방문지 목록. 배열 순서대로 `sortOrder` 저장 |
| `stops[].stopType` | enum | false | `sightseeing`, `meal`, `rest`, `cafe`. 기본값 `sightseeing` |
| `stops[].sourceProvider` | enum | false | `tour_api`, `tmap`, `kakao_map`, `public_data`, `local_excel`, `internal`. 기본값 `tour_api` |
| `stops[].externalPlaceId` | string | false | 외부 장소 ID. TourAPI contentId, 지도 API 장소 ID 등 |
| `stops[].contentTypeId` | string | false | TourAPI contentTypeId |
| `stops[].name` | string | true | 장소명 |
| `stops[].category` | string | false | 장소 카테고리 |
| `stops[].address` | string | false | 주소 |
| `stops[].latitude` | decimal | false | 위도 |
| `stops[].longitude` | decimal | false | 경도 |
| `stops[].phone` | string | false | 전화번호 |
| `stops[].homepageUrl` | string | false | 홈페이지 URL |
| `stops[].imageUrl` | string | false | 대표 이미지 URL |
| `stops[].overview` | string | false | 장소 소개 |
| `stops[].arrivalTime` | time | false | 도착 예정 시간 |
| `stops[].dwellMinutes` | number | false | 예상 체류 시간(분) |
| `stops[].note` | string | false | 방문지 메모 |
| `stops[].recommendationReason` | string | false | 추천 이유 |
| `stops[].recommendationTags` | string array | false | 추천 태그 |
| `stops[].sourcePayload` | object | false | 외부 API 원본 응답 일부. 후속 API 연동 전까지 유동 필드 보관용 |
| `stops[].isManualAdded` | boolean | false | 사용자가 직접 추가한 장소 여부 |

### Response

`GET /api/v1/trips/{tripId}/course`와 같은 `TripCourseResponse` 형태입니다.

같은 가족의 부모는 코스를 조회할 수 있지만 저장, 추천 재생성, 경로 최적화는 할 수 없습니다.
방문지 편집 흐름과 전체 덮어쓰기 기준은 `docs/policy/trip-edit.md`를 따릅니다.

---

## PUT /api/v1/trips/{tripId}/stops/{stopId}/note

여행을 만든 자녀가 방문지 메모를 즉시 저장하거나 삭제합니다. 코스 순서와 경로에는 영향을 주지 않습니다.

### Request

```json
{
  "note": "부모님과 사진 찍기"
}
```

| Field | Type | Required | 설명 |
|-------|------|----------|------|
| `note` | string | false | 최대 255자. `null` 또는 공백이면 기존 메모 삭제 |

응답 `data`는 수정된 `TripStopResponse`입니다. 여행을 만든 자녀만 `planning`, `ready`, `in_progress` 상태에서 호출할 수 있습니다.

---

## GET /api/v1/trips/{tripId}/nearby-cafes

여행 기간 중 같은 가족 구성원이 현재 위치 기준 5km 이내 카페를 가까운 순으로 최대 10개 조회합니다.
서버는 Tmap POI 주변 카테고리 검색의 `카페` 카테고리를 실시간으로 조회하며 결과를 DB에 저장하지 않습니다.

Query Parameters와 접근 정책은 주변 공중화장실 조회 API와 같습니다. 응답에는 Tmap POI ID, 이름, 주소, 좌표, 현재 위치와의 직선거리(m), 전화번호가 포함됩니다.

---

## GET /api/v1/trips/{tripId}/nearby-restrooms

여행 기간 중 같은 가족 구성원이 현재 위치 주변 공중화장실을 조회합니다.
클라이언트가 반경이나 개수를 지정하지 않으며, 서버가 직선거리 5km 이내에서 가까운 순으로 최대 10개를 반환합니다.

### Query Parameters

| Parameter | Type | Required | 설명 |
|-----------|------|----------|------|
| `latitude` | number | true | 현재 위치 WGS84 위도. `-90` 이상 `90` 이하 |
| `longitude` | number | true | 현재 위치 WGS84 경도. `-180` 이상 `180` 이하 |

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "주변 공중화장실 조회 성공",
  "data": [
    {
      "id": 1,
      "name": "사직단 공중화장실",
      "address": "서울특별시 종로구 사직동 1-28",
      "latitude": 37.5758692,
      "longitude": 126.9684817,
      "distanceMeters": 320,
      "phone": "02-2148-2832",
      "operatingHours": "상시"
    }
  ]
}
```

5km 이내 화장실이 없으면 `data`는 빈 배열입니다.
여행 시작 전과 종료 후에는 HTTP `200`/본문 `status=400`, 다른 가족 사용자에게는 HTTP `200`/본문 `status=403`을 반환합니다.

---

## GET /api/v1/trips/{tripId}/nearby-hospitals

여행 기간 중 같은 가족 구성원이 현재 위치 기준 5km 이내 병원을 가까운 순으로 최대 10개 조회합니다.
서버는 Tmap POI 주변 카테고리 검색의 `병원` 카테고리를 실시간으로 조회하며 결과를 DB에 저장하지 않습니다.
Tmap 분류상 의원·치과 등이 포함될 수 있고 응급실 운영 여부는 보장하지 않습니다.

Query Parameters와 접근 정책은 주변 공중화장실 조회 API와 같습니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "주변 병원 조회 성공",
  "data": [
    {
      "id": "12345678",
      "type": "hospital",
      "name": "서울대학교병원",
      "address": "서울 종로구 대학로 101",
      "latitude": 37.579617,
      "longitude": 126.998998,
      "distanceMeters": 320,
      "phone": "02-2072-2114"
    }
  ]
}
```

Tmap 호출·응답 처리 실패는 실제 HTTP `500`, 본문 `status=500`을 반환합니다.

---

## GET /api/v1/trips/{tripId}/nearby-pharmacies

여행 기간 중 같은 가족 구성원이 현재 위치 기준 5km 이내 약국을 가까운 순으로 최대 10개 조회합니다.
서버는 Tmap POI 주변 카테고리 검색의 `약국` 카테고리를 실시간으로 조회하며 결과를 DB에 저장하지 않습니다.

Query Parameters, 접근 정책, 응답 필드는 주변 병원 조회 API와 같고 `type`은 `pharmacy`, 성공 메시지는 `주변 약국 조회 성공`입니다.
