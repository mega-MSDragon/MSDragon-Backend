# 여행 경로 최적화 정책

여행 일자별 방문지 순서를 Tmap 경유지 순서 최적화 API로 계산하는 정책입니다.
사용자는 시작점과 도착점을 별도로 입력하지 않습니다.

---

## 권한과 상태

- API: `POST /api/v1/trips/{tripId}/days/{dayNumber}/route-optimization`
- 여행을 생성한 자녀만 방문 순서와 경로 캐시를 갱신할 수 있습니다.
- `planning`, `ready`, `in_progress` 상태에서 호출할 수 있습니다.
- `completed`, `stopped`, `archived` 상태에서는 호출할 수 없습니다.

---

## 결정 사항

- 사용자는 시작점/도착점을 입력하지 않습니다.
- 서버가 해당 일자의 모든 방문지를 시작점/도착점 후보로 순회합니다.
- 각 시작/도착 조합마다 Tmap `routeOptimization10`을 호출합니다.
- 나머지 방문지는 `viaPoints`로 전달합니다.
- `totalTime`이 가장 짧은 결과를 선택합니다.
- `totalTime`이 같으면 `totalDistance`가 더 짧은 결과를 선택합니다.
- 선택된 결과 기준으로 `trip_stops.sort_order`, `arrival_time`, `dwell_minutes` 기본값, `trip_days` 경로 캐시를 갱신합니다.

예시:

- 방문지: A, B, C, D
- 호출 후보: A->D(B,C 경유), A->C(B,D 경유), B->A(C,D 경유) ...
- 가장 짧은 결과의 순서를 최종 코스로 저장합니다.

---

## API 사용

| 항목 | 값 |
|------|----|
| API | Tmap 경유지 순서 최적화 10 |
| Method | `POST` |
| URL | `https://apis.openapi.sk.com/tmap/routes/routeOptimization10?version=1` |
| 인증 헤더 | `appKey: ${TMAP_APP_KEY}` |

기본 요청값:

| Field | 값 | 설명 |
|-------|----|------|
| `reqCoordType` | `WGS84GEO` | 요청 좌표 타입 |
| `resCoordType` | `WGS84GEO` | 응답 좌표 타입 |
| `searchOption` | `0` | 교통최적 + 추천 |
| `carType` | `1` | 승용차 |
| `coordinateFlag` | `0` | 경로 좌표 요청 |
| `startTime` | 여행일 + `10:00` | `YYYYMMDDHHMM` 형식 |

---

## 환경 변수

| Name | 설명 |
|------|------|
| `TMAP_APP_KEY` | Tmap API 앱키. 실제 값은 코드/문서에 커밋하지 않습니다. |
| `TMAP_SEARCH_OPTION` | 기본값 `0` |
| `TMAP_CAR_TYPE` | 기본값 `1` |
| `TMAP_DEFAULT_START_TIME` | 기본값 `10:00` |

---

## 대상 방문지 조건

- 하루 방문지가 3곳 이상이어야 합니다.
- 하루 방문지는 최대 10곳까지 최적화합니다.
- 모든 방문지에 위도/경도 좌표가 있어야 합니다.
- 2곳 이하의 단순 경로는 경유지 순서 최적화보다 자동차 경로안내 API가 적합하므로 이번 정책에서는 제외합니다.

---

## 기본 체류 시간

방문지에 `dwellMinutes`가 없으면 아래 기본값을 사용하고, 최적화 실행 시 방문지에도 저장합니다.

| stopType | 기본 체류 시간 |
|----------|----------------|
| `sightseeing` | 60분 |
| `meal` | 60분 |
| `rest` | 40분 |
| `cafe` | 40분 |

---

## 저장 규칙

`trip_days`에 일자 단위 경로 캐시를 저장합니다.

| Field | 설명 |
|-------|------|
| `route_provider` | `tmap` |
| `route_total_distance_m` | Tmap `totalDistance` |
| `route_total_duration_seconds` | Tmap `totalTime` |
| `route_polyline` | 지도 표시용 경로 좌표 목록 JSON |
| `route_source_payload` | Tmap 응답 properties와 선택된 시작/도착/순서 정책 JSON |
| `route_optimized_at` | 계산 시간 |

`trip_stops`에는 아래 값을 갱신합니다.

- 최적 순서 기준 `sort_order`
- Tmap point 응답 기준 `arrival_time`
- 비어 있던 `dwell_minutes` 기본값

---

## 무효화 규칙

아래 상황에서는 기존 경로 캐시가 더 이상 맞지 않으므로 삭제합니다.

- 코스 전체 저장 API로 해당 일자의 방문지가 추가/삭제/수정/순서 변경되는 경우
- 추천 코스 생성 API로 기존 코스를 덮어쓰는 경우
- 여행 날짜 변경 API가 추가되어 `trip_days.travel_date`가 바뀌는 경우

날짜가 변경되면 Tmap `startTime`도 달라지므로 경로를 다시 계산해야 합니다.
코스 전체 저장 요청에 포함되더라도 방문지 구성이 같은 일자는 방문지 ID와 기존 경로 캐시를 유지합니다.
