# Home Domain

Home 도메인은 확정된 홈 화면에 필요한 데이터를 섹션별 API로 제공합니다.

## 책임

- 로그인 사용자의 역할을 반환합니다. 여행 생성 가능 여부는 자녀 역할로 판단합니다.
- 부모 본인 또는 자녀에게 연결된 부모별 상세 프로필 완료 여부를 boolean으로 반환합니다.
- 진행 중·완료·예정 여행을 홈 카드 형태로 반환합니다.
- 완료 여행에 제출된 부모별 별점을 함께 반환합니다.
- 여행 생성 당시 부모 프로필 스냅샷으로 대표 테마와 여행 강도를 계산합니다.
- 축제 영역부터 아래까지를 동적 섹션 목록으로 제공합니다. 모든 섹션이 같은 카드로 그려져 섹션 추가·삭제·재정렬이 서버에서만 끝납니다. 구성과 순서는 `HomeSectionPolicy`가 정하고 `/festivals`는 deprecated로 유지합니다.
- 월별 고정 추천 도시 5개와 대표 이미지를 제공합니다. 이미지는 `resources/static/images/destinations/{code}.png` 12장을 정적 리소스로 서빙하며, 파일이 없는 도시만 TourAPI로 폴백합니다. `docs/policy/home.md`를 따릅니다.
- 국문 TourAPI에서 진행·예정 축제를 조회합니다.
- 추천 도시와 축제를 각각 일 단위로 캐시하고 외부 API 장애 및 지연을 다른 홈 섹션과 격리합니다.

## 패키지 구조

```text
home
├── controller
├── dto
├── service
└── tourapi
```

## 관련 테이블

Home 전용 테이블은 없습니다. 아래 기존 데이터를 읽어서 응답을 구성합니다.

- `users`
- `family_members`
- `parent_profiles`
- `trips`
- `trip_feedbacks`

## 관련 API

- `GET /api/v1/home/my-trips`
- `GET /api/v1/home/monthly-recommendations`
- `GET /api/v1/home/festivals`

## 구현 결정

- 기존 `GET /api/v1/trips`는 기록 등 다른 화면에서도 사용하므로 반환 범위를 변경하지 않습니다.
- 홈 화면 조회 계약은 나의 여행, 월별 추천 여행, 축제 단위로 분리합니다.
- 실제 푸시 알림 전송과 `나중에 하기` 서버 저장은 현재 범위에서 제외합니다.
- 완료 여행의 별점은 평균으로 합치지 않고 부모별 목록으로 반환합니다.
- 월별 추천 도시 정책과 축제 캐시에는 DB를 사용하지 않습니다.
