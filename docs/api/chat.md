# Travel Chat API

여행 기간에 사용하는 AI 챗봇 API입니다. 모든 API는 Bearer access token이 필요합니다.

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/v1/trips/{tripId}/chat/messages` | 내 여행별 대화 이력 조회 |
| `POST` | `/api/v1/trips/{tripId}/chat/messages` | 질문 전송 및 AI 답변 생성 |

두 API 모두 서울 날짜 기준 여행 시작일부터 종료일까지 같은 가족 구성원만 사용할 수 있습니다. 대화 세션은 사용자별·여행별로 분리합니다.

## GET /api/v1/trips/{tripId}/chat/messages

현재 사용자의 대화 이력을 시간순으로 반환합니다. `suggestedQuestions`는 화면에 표시할 추천 질문입니다. 아직 질문하지 않았다면 `sessionId=null`, `messages=[]`와 최초 추천 질문 3개를 반환합니다. 대화가 있으면 마지막 AI 답변의 맥락에 맞춘 추천 질문을 반환합니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 AI 대화 이력 조회 성공",
  "data": {
    "sessionId": 1,
    "messages": [
      {
        "id": 1,
        "sender": "user",
        "content": "오늘 첫 번째 방문지는 어디야?",
        "createdAt": "2026-08-06T11:30:00"
      },
      {
        "id": 2,
        "sender": "assistant",
        "content": "오늘 첫 번째 방문지는 첨성대입니다.",
        "createdAt": "2026-08-06T11:30:02"
      }
    ],
    "suggestedQuestions": [
      "첨성대는 어떤 곳이야?",
      "그다음 방문지는 어디야?"
    ]
  }
}
```

## POST /api/v1/trips/{tripId}/chat/messages

첫 질문이면 세션을 자동으로 생성합니다. 서버는 현재 여행 일정과 최근 대화 내용을 OpenAI Responses API에 전달한 뒤 질문과 답변을 함께 저장합니다. AI는 답변과 함께 현재 맥락에 맞춘 다음 추천 질문 2~3개를 생성합니다. 주변 시설 질문에는 현재 좌표를 함께 전달해야 실제 조회 결과를 받을 수 있습니다.

### Request

```json
{
  "message": "가까운 화장실 3곳 알려줘",
  "latitude": 35.1587,
  "longitude": 129.1604
}
```

| 필드 | 타입 | 필수 | 제한 |
|---|---|---|---|
| `message` | string | true | 공백 제외 1자 이상, 최대 500자 |
| `latitude` | number | false | WGS84 위도. 주변 시설 조회 시 longitude와 함께 전달 |
| `longitude` | number | false | WGS84 경도. 주변 시설 조회 시 latitude와 함께 전달 |

일정이나 방문지 질문에는 좌표가 필요 없습니다. 좌표는 OpenAI에 직접 전달하지 않고 서버 내부의 주변 시설 조회에만 사용합니다.

서버가 AI에 제공하는 조회 도구:

- 최신 여행 전체 일정 또는 특정 일차 조회
- 코스에 저장된 방문지 상세·메모 조회
- 현재 위치 주변 카페, 공중화장실, 병원, 약국 조회

주변 시설 결과의 `distanceMeters`는 직선거리입니다. 현재 API는 도보 경로 시간으로 변환하지 않습니다.

### Response

```json
{
  "status": 200,
  "success": true,
  "message": "여행 AI 답변 생성 성공",
  "data": {
    "sessionId": 1,
    "userMessage": {
      "id": 1,
      "sender": "user",
      "content": "오늘 첫 번째 방문지는 어디야?",
      "createdAt": "2026-08-06T11:30:00"
    },
    "assistantMessage": {
      "id": 2,
      "sender": "assistant",
      "content": "오늘 첫 번째 방문지는 첨성대입니다.",
      "createdAt": "2026-08-06T11:30:02"
    },
    "suggestedQuestions": [
      "첨성대는 어떤 곳이야?",
      "그다음 방문지는 어디야?"
    ]
  }
}
```

추천 질문 정책:

- 최초 추천 질문은 OpenAI를 호출하지 않고 서버 기본값 3개를 반환합니다.
- 답변 이후에는 같은 OpenAI 호출에서 답변 맥락에 맞춘 질문 2~3개를 함께 생성합니다.
- 추천 질문은 마지막 AI 메시지의 `metadata`에 저장하므로 대화 화면에 다시 진입해도 유지됩니다.
- 추천 질문을 선택할 때도 일반 질문과 동일하게 이 POST API의 `message`로 전송합니다.
- 주변 시설 추천 질문을 전송할 때는 현재 `latitude`, `longitude`를 함께 전달합니다.

## 오류

| 실제 HTTP | 본문 status | 상황 |
|---|---|---|
| `200` | `400` | 빈 질문, 500자 초과, 좌표 한쪽만 입력, 여행 기간 밖에서 사용 |
| `200` | `401` | access token 누락·만료·변조 |
| `200` | `403` | 다른 가족의 여행 접근 |
| `200` | `404` | 여행이 없음 |
| `500` | `500` | OpenAI API 키 누락, timeout, 외부 API 오류, 응답 해석 실패 |

클라이언트는 실제 HTTP가 `200`이면 본문의 `success`와 `status`를 확인합니다. OpenAI 연동 실패는 예상 가능한 정책 오류가 아닌 시스템 오류이므로 실제 HTTP `500`입니다.
