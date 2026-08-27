# Support Facility Domain

여행 모드에서 현재 위치 주변의 카페, 공중화장실, 병원, 약국을 조회합니다.

## 책임

- 행정안전부 공중화장실 CSV의 주소를 Tmap으로 좌표 변환해 `support_facilities`에 일회성 적재합니다.
- 같은 파일을 다시 적재해도 기존 원천 ID는 건너뜁니다.
- 여행 기간 중인 같은 가족 구성원에게 현재 위치 주변 공중화장실을 제공합니다.
- Tmap POI 주변 카테고리 검색으로 현재 위치 주변 카페, 병원, 약국을 실시간 제공합니다.
- 카페는 한국관광공사 위치 기반 음식점 데이터와 안전하게 일치하는 경우 썸네일을 보강합니다.
- 현재 위치와 시설 좌표의 직선거리(Haversine)를 계산해 5km 이내를 가까운 순으로 정렬하고, 4개 API 모두 응답 항목마다 `distanceMeters`(미터 단위 정수)로 반환합니다. `km` 변환은 클라이언트가 표시 시점에 처리합니다.

## 패키지 구조

```text
supportfacility
├── controller
├── dto
├── entity
├── importer
├── repository
├── service
└── tmap
```

## 관련 테이블

- `support_facilities`

## 관련 API

- `GET /api/v1/trips/{tripId}/nearby-cafes`
- `GET /api/v1/trips/{tripId}/nearby-restrooms`
- `GET /api/v1/trips/{tripId}/nearby-hospitals`
- `GET /api/v1/trips/{tripId}/nearby-pharmacies`

엔드포인트가 `/trips/{tripId}` 하위이므로 요청·응답 상세는 `docs/api/trip.md`의 `주변 시설 조회 공통` 절과 각 API 절에 있습니다. `docs/api/supportfacility.md`는 두지 않습니다.

## 구현 결정

- 클라이언트는 검색 반경과 개수를 입력하지 않습니다.
- 서버가 현재 위치 기준 직선거리 5km 이내 시설을 가까운 순으로 최대 10개 반환합니다.
- 조회 결과가 없으면 빈 배열을 반환합니다.
- CSV 적재는 서버 시작 시 자동 실행하지 않고 운영 명령으로만 실행합니다.
- 원천 `provider`는 `local_excel`, 좌표 변환 제공자는 원본 JSON의 `geocodingProvider=tmap`으로 기록합니다.
- 카페·병원·약국은 각각 Tmap `카페`, `병원`, `약국` 카테고리를 5km·10개·거리순으로 조회하며 DB에 저장하지 않습니다.
- 카페 썸네일 보강 실패는 카페 목록 조회 실패로 전파하지 않습니다.
- 적재와 조회 기준은 `docs/policy/nearby-support-facilities.md`를 따릅니다.
