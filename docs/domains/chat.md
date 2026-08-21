# Chat Domain

여행 모드에서 사용자별 AI 대화를 관리하고 OpenAI Responses API로 답변을 생성합니다.

## 책임

- 여행 기간과 가족 접근 권한을 검증합니다.
- 사용자별·여행별 활성 세션을 첫 질문에 자동 생성합니다.
- 첫 질문 전에는 DB에 저장하지 않는 AI 인사말 한 건을 조회 응답에 포함합니다.
- 질문과 AI 답변을 시간순으로 저장합니다.
- 현재 여행 일정과 방문지를 AI 문맥으로 구성합니다.
- Responses API function calling으로 일정·방문지·주변 시설을 서버에서 조회합니다.
- 여행 관련 정보가 서버 데이터만으로 부족하면 Responses API web search로 보강합니다.
- 최근 20개 메시지를 OpenAI에 전달합니다.
- 최초 추천 질문과 답변 맥락에 맞춘 다음 추천 질문을 제공합니다.
- OpenAI 호출 실패를 시스템 오류로 변환합니다.

## 패키지 구조

```text
chat
├── config
├── controller
├── dto
├── entity
├── openai
├── repository
└── service
```

## 관련 테이블

- `chat_sessions`
- `chat_messages`

## 관련 API

- `GET /api/v1/trips/{tripId}/chat/messages`
- `POST /api/v1/trips/{tripId}/chat/messages`

## 구현 결정

- OpenAI SDK를 추가하지 않고 기존 외부 API 연동과 같은 Java `HttpClient`를 사용합니다.
- 기본 모델은 `gpt-5.6-luna`이며 `OPENAI_MODEL`로 변경할 수 있습니다.
- 부모 프로필, 여행 MBTI, 가족 구성원 정보는 이번 버전의 AI 문맥에 포함하지 않습니다.
- 현재 좌표는 모델에 전달하지 않고 카페·화장실·의료시설 도구 실행에만 사용합니다.
- 최초 추천 질문은 서버 기본값으로 제공하고, 답변 이후 추천 질문 2~3개는 구조화된 AI 응답으로 답변과 함께 생성합니다.
- 최초 인사말의 `id`, `createdAt`은 `null`이며 실제 대화 이력에는 저장하지 않습니다.
- 추천 질문은 별도 테이블 없이 AI 메시지의 기존 `metadata`에 저장합니다.
- 세부 정책은 `docs/policy/travel-chat.md`를 따릅니다.
