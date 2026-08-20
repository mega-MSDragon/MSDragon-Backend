# 여행 모드 주변 편의시설 정책

현재 구현은 카페, 공중화장실, 병원, 약국을 다룹니다. 공중화장실은 DB 적재 데이터를 사용하고 카페·병원·약국은 Tmap에서 실시간 조회합니다.

## 조회 기준

- 여행 시작일부터 종료일까지 같은 가족 구성원 모두가 조회할 수 있습니다.
- 클라이언트는 현재 위치의 WGS84 `latitude`, `longitude`만 전송합니다.
- 검색 반경과 결과 개수는 클라이언트 요청값으로 받지 않습니다.
- 서버는 현재 위치 기준 직선거리 5km 이내 시설을 가까운 순으로 최대 10개 반환합니다.
- 5km 안에 시설이 없으면 멀리 있는 시설을 대신 반환하지 않고 빈 배열을 반환합니다.
- 서버는 모든 시설의 좌표로 Haversine 직선거리를 다시 계산해 거리순과 5km 범위를 보정합니다.

## 병원·약국 실시간 조회

- 병원은 `GET /api/v1/trips/{tripId}/nearby-hospitals`, 약국은 `GET /api/v1/trips/{tripId}/nearby-pharmacies`로 분리합니다.
- Tmap `명칭(POI) 주변 카테고리 검색` API를 사용합니다.
- URL은 `GET https://apis.openapi.sk.com/tmap/pois/search/around`입니다.
- `categories`는 각각 `병원`, `약국`을 사용하고 `radius=5`, `count=10`, `sort=distance`로 요청합니다.
- 요청·응답 좌표계는 `WGS84GEO`입니다.
- Tmap POI ID, 이름, 주소, 입구점 우선 좌표, 전화번호를 반환합니다.
- Tmap `병원` 카테고리는 종합병원뿐 아니라 의원·치과 등을 포함하며 응급실 운영 여부를 제공하지 않습니다. 응급실 전용 조회가 필요해지면 별도 의료 데이터 제공자를 사용해야 합니다.
- 병원·약국 결과는 `support_facilities`에 저장하지 않습니다. API 호출 시점의 결과만 클라이언트에 전달합니다.
- 별도 환경변수 없이 기존 `TMAP_APP_KEY`와 Tmap timeout 설정을 재사용합니다.

## 카페 실시간 조회

- 카페는 `GET /api/v1/trips/{tripId}/nearby-cafes`로 조회합니다.
- Tmap `명칭(POI) 주변 카테고리 검색` API에 `categories=카페`, `radius=5`, `count=10`, `sort=distance`로 요청합니다.
- 병원·약국과 같은 응답 좌표·거리 계산 정책을 적용하고 DB에는 저장하지 않습니다.

## 공중화장실 조회

- DB 조회는 먼저 위도·경도 bounding box로 후보를 줄이고, Haversine 공식으로 실제 직선거리를 계산합니다.

## 원천 데이터

- 원천은 행정안전부 공중화장실 CSV이며 파일 인코딩은 CP949입니다.
- 시설 식별자는 `개방자치단체코드:관리번호`로 만들고 `facility_type + provider + source_id`를 unique로 유지합니다.
- 원천 `provider`는 `local_excel`입니다.
- 도로명주소를 우선하고, 비어 있으면 지번주소를 사용합니다.
- 전화번호, 개방시간, 원본 CSV row를 함께 저장합니다.

## 좌표 변환

- 현재 공공데이터 원본은 2025년 2월부터 WGS84 위도·경도를 제공하지 않아 Tmap `Full Text Geocoding`을 사용합니다.
- URL은 `GET https://apis.openapi.sk.com/tmap/geo/fullAddrGeo`입니다.
- 요청은 `addressFlag=F00`, `coordType=WGS84GEO`, `count=1`을 사용합니다.
- 좌표 우선순위는 새주소 입구점, 새주소 중심점, 지번주소 입구점, 지번주소 중심점 순서입니다.
- 변환 결과가 없거나 대한민국 범위를 벗어나면 해당 시설을 적재하지 않습니다.

## 일회성 적재

- 적재기는 서버 시작 시 자동 실행하지 않습니다.
- 성공한 시설은 100건씩 즉시 저장합니다.
- 다시 실행하면 DB에 이미 있는 원천 ID를 건너뛰므로 호출 제한이나 오류 뒤에도 이어서 처리할 수 있습니다.
- API 오류가 발생하면 현재 배치를 저장한 뒤 작업을 중단합니다.
- 잘못된 row, 좌표 검색 실패, API 오류 row는 원본 파일 옆의 `{파일명}-failures.csv`에 UTF-8로 기록합니다.
- Tmap 무료 지오코딩 호출 한도에 걸리면 다음 날 같은 명령을 다시 실행합니다.

## Tmap 저장 제한 관련 결정

Tmap 공식 약관에는 API 결과를 저장한 뒤 24시간 이상 사용할 수 없다는 제한이 있습니다.
현재 공모전 MVP에서는 사용자의 운영 결정에 따라 일회성 좌표 변환 결과를 DB에 유지합니다.
공모전 이후 서비스를 계속 운영하거나 외부에 정식 출시하기 전에는 좌표 포함 공공데이터 또는 저장 가능한 좌표 제공자로 반드시 교체해야 합니다.
병원·약국 POI 결과는 저장하지 않으므로 이 예외 결정의 대상이 아닙니다.

- Tmap 약관: https://tmapapi.tmapmobility.com/terms.html
- Tmap Full Text Geocoding 문서: https://tmapapi.tmapmobility.com/webservice/docs/fullTextGeocoding.html
- Tmap POI 주변 카테고리 검색 문서: https://tmapapi.tmapmobility.com/main.html#webservice/docs/tmapPoiAroundSearch
- 행정안전부 공중화장실 데이터: https://www.data.go.kr/data/15012892/standard.do
