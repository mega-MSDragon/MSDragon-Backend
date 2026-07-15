# Pledge Domain

Pledge 도메인은 여행 10계명 템플릿 후보군과 여행별 확정 문구를 담당합니다.

## 책임

- 서버 기본 템플릿 후보군을 DB에 누락 없이 초기화합니다.
- 여행을 만든 자녀에게 활성 후보 중 중복 없는 10개를 무작위로 제공합니다.
- 수정 완료한 문구 10개를 여행별 확정본으로 저장하고 다시 조회합니다.
- 템플릿 원문 사용 여부와 화면 표시 순서를 함께 저장합니다.
- 서명 요청 이후에는 문구가 변경되지 않도록 상태 기반 수정 제한을 적용합니다.
- 참여자별 PNG 서명 바이트를 저장하고 모든 여행 참여자에게 전체 서명을 제공합니다.
- 자녀 선서명과 부모 최소 1명 서명 완료 상태를 관리합니다.

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
- `pledge_signatures`

## 관련 API

- `GET /api/v1/trips/{tripId}/pledge/candidates`
- `GET /api/v1/trips/{tripId}/pledge`
- `PUT /api/v1/trips/{tripId}/pledge`
- `POST /api/v1/trips/{tripId}/pledge/signatures/me`

## 구현 결정

- 후보 조회 결과는 저장하지 않으며 클라이언트가 수정 중 상태를 관리합니다.
- 확정본은 여행당 하나만 두고 `PUT` 호출 시 전체 항목을 교체합니다.
- 서명은 PNG Base64 요청을 디코딩해 DB `bytea`에 저장하며 별도 파일 저장소를 사용하지 않습니다.
- 모든 여행 참여자는 현재까지 제출된 전체 서명을 동일하게 조회합니다.
- 첫 참여 부모 서명으로 `completed`가 되어도 다른 참여 부모의 추가 서명을 허용합니다.
- PDF 공유 가능 조건은 자녀 서명과 여행 참여 부모 최소 1명의 서명 완료입니다.
- HTML/PDF 완성본은 저장하지 않고 디자인 확정 후 요청 시 생성하도록 구현합니다.
