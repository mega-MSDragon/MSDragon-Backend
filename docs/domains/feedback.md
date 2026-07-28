# Feedback Domain

Feedback 도메인은 여행 마지막 날부터 진행하는 부모 평가 요청과 부모별 여행 소감 제출을 담당합니다.

## 책임

- 여행 생성 자녀가 미제출 참여 부모 전체에게 보내는 평가 요청을 부모별 한 건으로 기록합니다.
- 참여 부모의 전체 만족도, 몸 상태, 태그, 베스트 장소, 자유 의견을 여행별 한 건으로 저장합니다.
- 제출 시점, 권한, 점수 단위, 태그 그룹, 베스트 장소 소속을 검증합니다.
- 참여 부모별 요청·제출 현황을 제공하고 마지막 부모 제출 시 효도 리포트 생성을 호출합니다.
- 여행 기간 또는 참여 부모가 바뀌면 기존 요청과 피드백을 초기화합니다.

## 패키지 구조

```text
feedback
├── controller
├── dto
├── entity
├── repository
└── service
```

## 관련 테이블

- `trip_feedback_requests`
- `trip_feedbacks`
- `trip_feedback_tags`

## 관련 API

- `POST /api/v1/trips/{tripId}/feedback/requests`
- `GET /api/v1/trips/{tripId}/feedback/status`
- `POST /api/v1/trips/{tripId}/feedback/me`
- `GET /api/v1/trips/{tripId}/feedback/me`

## 구현 결정

- 평가 요청은 자녀가 여러 번 호출해도 부모별 한 건만 유지합니다.
- 부모는 요청을 받기 전에도 마지막 날부터 제출할 수 있습니다.
- 부모별 제출은 한 번만 허용하고 수정 API를 두지 않습니다.
- 별점은 `0.0`부터 `5.0`까지 `0.5` 단위로 저장합니다.
- 베스트 장소는 제출 당시 방문지 ID와 장소명 스냅샷을 저장하고 FK는 두지 않습니다.
- 모든 참여 부모가 제출하면 효도 리포트를 즉시 생성하고 응답의 `reportReady`를 `true`로 반환합니다.
