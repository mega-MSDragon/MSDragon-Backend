# Pledge Domain

Pledge 도메인은 여행 10계명 화면 데이터, 저장 문구와 참여자 서명을 담당합니다.

## 책임

- 서버 기본 템플릿 후보군을 DB에 누락 없이 초기화합니다.
- 저장된 10계명이 없으면 화면 조회 API에서 활성 후보 중 중복 없는 10개를 무작위로 제공합니다.
- 수정한 문구 10개를 여행별로 저장하고 같은 화면 조회 API에서 다시 제공합니다.
- 템플릿 원문 사용 여부와 화면 표시 순서를 함께 저장합니다.
- 서명 요청 이후에는 문구가 변경되지 않도록 상태 기반 수정 제한을 적용합니다.
- 참여자별 PNG 서명 바이트를 저장하고 모든 여행 참여자에게 전체 서명을 제공합니다.
- 자녀 선서명과 부모 최소 1명 서명 완료 상태를 관리합니다.
- 저장된 문구와 현재 서명 현황을 HTML 템플릿에 합성해 모바일 비율 PDF 원본으로 제공합니다.
- 여행의 참여 부모 구성이 바뀌면 기존 저장 문구와 모든 서명을 삭제해 새 구성에서 다시 작성하도록 합니다.

## 패키지 구조

```text
pledge
├── config
├── controller
├── dto
├── entity
├── repository
└── service

src/main/resources
├── fonts
└── templates/pledge
```

## 관련 테이블

- `pledge_templates`
- `trip_pledges`
- `pledge_items`
- `pledge_signatures`

## 관련 API

- `GET /api/v1/trips/{tripId}/pledge/candidates`
- `GET /api/v1/trips/{tripId}/pledge`
- `GET /api/v1/trips/{tripId}/pledge/pdf`
- `PUT /api/v1/trips/{tripId}/pledge`
- `POST /api/v1/trips/{tripId}/pledge/signatures/me`

## 구현 결정

- `GET /pledge`는 저장 전 후보부터 서명 완료 결과까지 10계명 화면 전체 데이터를 반환합니다.
- 저장 전 후보 조회 결과는 저장하지 않으며 클라이언트가 수정 중 상태를 관리합니다.
- 저장본은 여행당 하나만 두고 `PUT` 호출 시 전체 항목을 교체합니다.
- `GET /pledge/candidates`는 호환성을 위해 남겨 둔 미사용 API이며 신규 클라이언트는 호출하지 않습니다.
- 서명은 PNG Base64 요청을 디코딩해 DB `bytea`에 저장하며 별도 파일 저장소를 사용하지 않습니다.
- 모든 여행 참여자는 현재까지 제출된 전체 서명을 동일하게 조회합니다.
- 첫 참여 부모 서명으로 `completed`가 되어도 다른 참여 부모의 추가 서명을 허용합니다.
- PDF는 10계명 문구를 한 번 저장했다면 서명 전에도 생성할 수 있습니다. 완료 전 PDF에는 도장을 표시하지 않고 미서명자의 빈 서명란을 표시합니다.
- HTML/PDF 완성본은 저장하지 않고 `GET /pledge/pdf` 요청마다 현재 데이터를 기준으로 생성합니다.
- 화면 조회 API는 제출된 `signatures`와 별도로 미서명 참여자를 포함한 `signers`를 반환해 부모 2명 서명 화면을 구성합니다.
- PDF는 부모와 자녀를 두 그룹으로 배치하고 부모 2명은 부모 그룹 안의 두 서명 칸으로 표시합니다.
- 임시 HTML은 `templates/pledge/trip-pledge.html`에 두며, 최종 디자인도 정책 문서의 슬롯 ID 계약을 유지해 교체합니다.
- HTML 정규화와 안전한 텍스트 삽입에는 Jsoup, PDF 변환에는 OpenHTMLtoPDF와 PDFBox를 사용합니다.
- Linux/Alpine 배포 환경에서도 한글이 깨지지 않도록 Nanum Gothic Regular/Bold와 OFL 라이선스를 애플리케이션 리소스에 포함합니다.
- 제목, 도시, 날짜 변경만으로는 10계명을 초기화하지 않고 참여 부모 ID 집합이 실제로 바뀔 때만 전체 삭제합니다.
