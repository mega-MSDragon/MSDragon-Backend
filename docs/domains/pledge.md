# Pledge Domain

Pledge 도메인은 여행 10계명 템플릿 후보군과 여행별 확정 문구를 담당합니다.

## 책임

- 서버 기본 템플릿 후보군을 DB에 누락 없이 초기화합니다.
- 여행을 만든 자녀에게 활성 후보 중 중복 없는 10개를 무작위로 제공합니다.
- 수정 완료한 문구 10개를 여행별 확정본으로 저장하고 다시 조회합니다.
- 템플릿 원문 사용 여부와 화면 표시 순서를 함께 저장합니다.
- 서명 요청 이후에는 문구가 변경되지 않도록 상태 기반 수정 제한을 적용합니다.

## 패키지 구조

```text
pledge
├── config
├── controller
├── dto
├── entity
├── repository
└── service
```

## 관련 테이블

- `pledge_templates`
- `trip_pledges`
- `pledge_items`
- 후속 구현: `pledge_signatures`

## 관련 API

- `GET /api/v1/trips/{tripId}/pledge/candidates`
- `GET /api/v1/trips/{tripId}/pledge`
- `PUT /api/v1/trips/{tripId}/pledge`

## 구현 결정

- 후보 조회 결과는 저장하지 않으며 클라이언트가 수정 중 상태를 관리합니다.
- 확정본은 여행당 하나만 두고 `PUT` 호출 시 전체 항목을 교체합니다.
- 이번 구현은 `reviewed` 단계까지 담당합니다.
- 서명 이미지 저장, 부모 서명 요청, 비트맵 렌더링, PDF 공유는 파일 저장소 정책 확정 후 이어서 구현합니다.
- PDF 공유 가능 조건은 자녀 서명과 여행 참여 부모 최소 1명의 서명 완료입니다.
