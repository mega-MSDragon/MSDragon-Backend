# 여행 코스 추천 정책

여행 생성 시 저장한 부모님 프로필 스냅샷과 한국관광공사 TourAPI 무장애여행 정보를 기반으로 일자별 방문지를 추천하는 정책입니다.
이번 정책은 장소 추천까지만 다루며, Tmap 경로/거리/소요시간 계산은 별도 작업에서 추가합니다.

## API 사용

기본 서비스는 한국관광공사 국문 무장애여행 서비스 `KorWithService2`를 사용합니다.

| 목적 | Operation | URL | 사용 시점 |
|------|-----------|-----|-----------|
| 지역별 장소 후보 조회 | `areaBasedList2` | `https://apis.data.go.kr/B551011/KorWithService2/areaBasedList2` | 추천 코스 생성 시 도시, 콘텐츠 타입별 후보 수집 |
| 키워드 장소 검색 | `searchKeyword2` | `https://apis.data.go.kr/B551011/KorWithService2/searchKeyword2` | 코스 편집 화면에서 도시 범위 안의 방문지 후보 검색 |
| 장소 공통 상세 조회 | `detailCommon2` | `https://apis.data.go.kr/B551011/KorWithService2/detailCommon2` | 최종 선택된 장소의 홈페이지, 소개 문구 보강 |
| 무장애 정보 조회 | `detailWithTour2` | `https://apis.data.go.kr/B551011/KorWithService2/detailWithTour2` | 이동 도움 필요 부모가 있으면 후보 점수 계산에 반영하고, 최종 선택 장소 스냅샷에 저장 |
| 법정동 코드 확인 | `ldongCode2` | `https://apis.data.go.kr/B551011/KorWithService2/ldongCode2` | 여행 도시 catalog를 TourAPI 법정동 코드로 매핑할 때 기준으로 사용 |
| 분류체계 코드 확인 | `lclsSystmCode2` | `https://apis.data.go.kr/B551011/KorWithService2/lclsSystmCode2` | 부모님 취향/MBTI를 TourAPI 분류체계 대분류로 매핑할 때 기준으로 사용 |

공통 파라미터:

| 파라미터 | 값 |
|----------|----|
| `serviceKey` | 서버 환경변수 `TOUR_API_SERVICE_KEY` |
| `MobileOS` | `ETC` |
| `MobileApp` | 환경변수 `TOUR_API_MOBILE_APP`, 기본값 `MSDragon` |
| `_type` | `json` |

## 후보 콘텐츠 타입

숙박은 부모 프로필 기반 추천 장소에서 제외합니다.
코스 편집용 키워드 검색도 같은 콘텐츠 타입만 허용하며, 사용자가 콘텐츠 타입을 지정하지 않으면 아래 타입 전체를 대상으로 후처리 필터링합니다.

| contentTypeId | 의미 | 저장 stopType |
|---------------|------|---------------|
| `12` | 관광지 | `sightseeing` |
| `14` | 문화시설 | `sightseeing` |
| `15` | 행사/공연/축제 | `sightseeing` |
| `28` | 레포츠 | `sightseeing` |
| `38` | 쇼핑 | `sightseeing` |
| `39` | 음식점 | `meal` |

## 여행 도시 매핑

TourAPI 2025년 매뉴얼 기준으로 legacy `areaCode` 대신 법정동 코드 `lDongRegnCd`, `lDongSignguCd`를 사용합니다.
광역 단위 도시는 시도 코드만 사용하고, 여러 도시를 묶은 catalog는 관련 시군구를 모두 조회합니다.

| destinationCode | 조회 지역 |
|-----------------|-----------|
| `daegu` | `27` 대구광역시 |
| `gangneung_sokcho` | `51-150` 강릉시, `51-210` 속초시 |
| `gyeongju` | `47-130` 경주시 |
| `busan` | `26` 부산광역시 |
| `yeosu` | `46-130` 여수시 |
| `incheon` | `28` 인천광역시 |
| `jeonju` | `52-111` 전주시 완산구, `52-113` 전주시 덕진구 |
| `jeju` | `50-110` 제주시, `50-130` 서귀포시 |
| `seoul` | `11` 서울특별시 |
| `suwon_yongin` | `41-111`, `41-113`, `41-115`, `41-117`, `41-461`, `41-463`, `41-465` |
| `tongyeong_geoje_namhae` | `48-220` 통영시, `48-310` 거제시, `48-840` 남해군 |
| `pohang_andong` | `47-111` 포항시 남구, `47-113` 포항시 북구, `47-170` 안동시 |

## 부모 프로필 매핑

TourAPI 분류체계 대분류 기준으로 부모 프로필 신호를 매핑합니다.

| 부모 입력 | TourAPI 분류체계 |
|-----------|------------------|
| 자연·풍경 | `NA` |
| 역사·문화 | `HS`, `VE` |
| 쇼핑 | `SH` |
| 액티비티 | `LS` |
| 문화생활 | `VE`, `EV` |
| 랜드마크 | `VE`, `HS`, `NA` |
| 체험 | `EX` |
| 도시 취향 탐험가 | `SH`, `VE`, `FD` |
| 감성 문화 산책가 | `VE`, `FD` |
| 유유자적 힐링러 | `NA`, `FD` |
| 역사 산책가 | `HS`, `NA`, `FD` |
| 액티비티 열정가 | `LS`, `EX`, `FD` |
| 로컬 도전가 | `EX`, `FD` |

## 일자별 장소 수

여행 기간 제한은 두지 않습니다. 여행 일수만큼 반복해서 일자별 코스를 생성합니다.
부모가 2명인 경우 더 천천히 걷는 부모 기준으로 하루 장소 수를 정합니다.

| walkingPace | 하루 추천 장소 수 |
|-------------|-------------------|
| `slow` | 3곳 |
| `normal` | 4곳 |
| `fast` | 5곳 |

음식점 후보가 있으면 각 일자에 식사 장소 1곳을 포함합니다.

## 가중치

기본 점수는 10점입니다.

| 조건 | 점수 |
|------|------|
| 부모 여행 테마와 TourAPI 분류체계 일치 | 부모 1명당 +15 |
| 부모 여행 MBTI와 TourAPI 분류체계 일치 | 부모 1명당 +8 |
| 음식점이고 음식 취향 `korean` | +8, 한식 키워드가 있으면 +12 |
| 음식점이고 음식 취향 `familiar` | +8 |
| 음식점이고 음식 취향 `adventurous` | +6 |
| 대표 이미지 있음 | +2 |
| `slow`이고 `NA`, `HS`, `VE`, `FD` | +4 |
| `slow`이고 `LS` | -8 |
| `normal`이고 `NA`, `HS`, `VE`, `FD` | +2 |
| `fast`이고 `LS`, `EX`, `EV` | +6 |

이동 도움이 필요한 부모가 한 명이라도 있으면 무장애 정보 문자열이 비어 있지 않은지를 기준으로 추가 점수를 줍니다.

| 무장애 필드 | 점수 |
|-------------|------|
| `route` | +12 |
| `exit` | +12 |
| `restroom` | +10 |
| `wheelchair` | +8 |
| `elevator` | +8 |
| `parking` | +6 |
| `publictransport` | +4 |
| 주요 무장애 필드가 모두 비어 있음 | -8 |

## 저장 규칙

- 추천 생성 API는 기존 `trip_stops`를 삭제하고 새 추천 결과로 덮어씁니다.
- 장소 원본이 바뀌어도 여행 당시 코스가 유지되도록 `trip_stops`에 장소 스냅샷을 저장합니다.
- `sourcePayload`에는 TourAPI 목록 응답, 상세 응답, 무장애 응답, 추천 점수를 저장합니다.
- 추천 생성이 완료되면 여행 상태가 `planning`인 경우 `ready`로 변경합니다.
- 도착시간, 이동거리, 경로 순서 최적화는 저장하지 않습니다. 해당 값은 Tmap 연동 작업에서 보강합니다.

## 코스 편집 검색 규칙

- 방문지 검색 API는 여행의 `destinationCode`를 TourAPI 법정동 코드로 변환한 뒤 `searchKeyword2`를 호출합니다.
- 여러 지역을 묶은 destination은 각 지역을 조회한 뒤 `contentId` 기준으로 중복 제거합니다.
- 검색 결과는 `contentTypeId`가 지원 타입인 장소만 내려줍니다. 숙박은 제외합니다.
- 검색 상세 API는 `detailCommon2`와 `detailWithTour2`를 조회해 장소 표시 정보, 무장애 주요 문자열, 원본 응답 일부를 내려줍니다.
- 검색/상세 API는 `trip_stops`를 직접 수정하지 않습니다. 실제 반영은 클라이언트가 선택한 장소들을 `PUT /api/v1/trips/{tripId}/course`로 저장할 때 이루어집니다.
