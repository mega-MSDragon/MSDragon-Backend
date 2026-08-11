# Database Schema

DB 스키마와 공통 엔티티 규칙을 기록합니다.

---

## 현재 상태

인증/가족/부모 프로필/여행/여행 10계명/여행 피드백/효도 리포트/여행 AI 챗봇 도메인 Entity가 도입되었습니다.
`docs/database/schema.dbml`과 `docs/database/erd.md`는 PDF 확정 와이어프레임 기준 목표 설계를 포함하므로,
현재 구현 Entity보다 앞서 있는 테이블과 컬럼이 있을 수 있습니다.

| Entity | Table | 설명 |
|--------|-------|------|
| `User` | `users` | 소셜 로그인 사용자와 회원가입 완료 프로필 |
| `UserConsent` | `user_consents` | 회원가입 약관 종류·버전별 동의 결정 |
| `UserRefreshToken` | `user_refresh_tokens` | 해시로 저장하는 refresh token 세션 |
| `Family` | `families` | 대표 자녀 기준 가족 |
| `FamilyMember` | `family_members` | 가족 구성원 |
| `FamilyCode` | `family_codes` | 사용자별 고정 가족 초대 코드 |
| `FamilyCodeUsage` | `family_code_usages` | 가족 코드 매칭 이력 |
| `ParentProfile` | `parent_profiles` | 부모님 상세 프로필과 추천용 여행 MBTI 현재값 |
| `ParentProfile.travelThemes` | `parent_profile_themes` | 부모님 프로필별 선호 여행 테마 enum code |
| `Trip` | `trips` | 여행 기본 정보 |
| `TripParticipant` | `trip_participants` | 여행 참여자 |
| `TripDay` | `trip_days` | 여행 일자 |
| `TripStop` | `trip_stops` | 여행 일자별 방문지 코스와 장소 스냅샷 |
| `PledgeTemplate` | `pledge_templates` | 여행 10계명 서버 후보 문구 |
| `TripPledge` | `trip_pledges` | 여행별 10계명 확정본과 진행 상태 |
| `PledgeItem` | `pledge_items` | 여행별 확정 문구 10개 |
| `PledgeSignature` | `pledge_signatures` | 참여자별 여행 10계명 PNG 서명 바이트 |
| `TripFeedbackRequest` | `trip_feedback_requests` | 자녀가 참여 부모에게 보낸 평가 요청 |
| `TripFeedback` | `trip_feedbacks` | 부모별 여행 피드백 1건 |
| `TripFeedback.tags` | `trip_feedback_tags` | 피드백에서 선택한 좋았던 점·개선점 태그 |
| `FilialReport` | `filial_reports` | 부모 피드백 완료 시 자동 생성하는 여행별 효도 리포트 |
| `SupportFacility` | `support_facilities` | 여행 모드 주변 공중화장실 등 편의시설 좌표 |
| `ChatSession` | `chat_sessions` | 사용자별·여행별 AI 채팅 세션과 여행 문맥 스냅샷 |
| `ChatMessage` | `chat_messages` | 사용자 질문과 AI 답변 이력 |

---

## 공통 엔티티 기반

생성/수정 시간 추적이 필요한 JPA Entity에는 `BaseTimeEntity`를 사용합니다.
현재 구현은 JPA Auditing 설정 없이 `@PrePersist`, `@PreUpdate`로 시간을 기록합니다.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `createdAt` | `LocalDateTime` | false | 생성 시간 |
| `updatedAt` | `LocalDateTime` | false | 수정 시간 |

---

## 인증 테이블 구현 메모

### users

- `oauth_provider + oauth_subject`는 unique입니다.
- DB 저장 enum 값은 DBML과 맞춰 소문자 문자열을 사용합니다.
- 회원가입 전 사용자는 저장하지 않습니다. 소셜 로그인 후 `signupToken`을 발급하고, 회원가입 완료 시 `users` row를 생성합니다.
- 회원 탈퇴는 soft delete로 처리합니다. `deleted_at`을 기록하고 소셜 식별자와 기본 프로필을 익명화해 같은 소셜 계정의 새 가입을 허용합니다.

### user_consents

- 개인정보 수집 및 이용은 필수, 위치 기반 편의시설 안내는 선택 약관입니다.
- 최초 약관 버전은 각각 `v1`이며 서버가 결정합니다.
- `(user_id, consent_type, terms_version)`은 unique이며 약관 개정 시 기존 행을 덮어쓰지 않고 새 버전 결정을 추가합니다.
- 선택 약관에 동의하지 않은 경우도 `agreed=false`로 결정을 기록합니다.

### user_refresh_tokens

- refresh token 원문은 저장하지 않고 SHA-256 해시를 저장합니다.
- 재발급 시 기존 refresh token은 `revoked_at`을 기록하고 새 refresh token을 발급합니다.
- `platform`은 요청 앱 플랫폼(`ios`, `android`, `web`)을 선택적으로 기록합니다.
- `device_id`는 현재 요구사항에서 사용하지 않아 두지 않습니다. 기기별 세션 관리가 필요해질 때 추가합니다.
- 회원 탈퇴 시 아직 폐기되지 않은 refresh token은 모두 `revoked_at`을 기록합니다.

---

## 가족 테이블 구현 메모

### families

- `owner_user_id`는 대표 자녀 사용자입니다.
- 가족은 자녀 쪽 가족을 기준으로 생성합니다.
- 대표 자녀가 탈퇴하면 `is_active=false`로 변경하고 가족 연결을 모두 해제합니다.

### family_members

- `user_id`는 unique입니다. 한 사용자는 하나의 가족에만 속합니다.
- `relation_label` 컬럼은 남겨두지만 현재 가족 매칭 API에서는 입력받지 않습니다. 가족 응답의 `relationLabel`은 부모 `users.gender` 기준으로 서버에서 계산합니다.
- 가족당 자녀 1명, 부모 최대 2명 제약은 서비스에서 검증합니다.
- 운영 DB에서는 DBML/ERD 설계처럼 자녀 1명 partial unique index와 부모 최대 2명 trigger를 추가 검토합니다.
- 부모 탈퇴는 해당 부모의 연결만 삭제합니다. 대표 자녀 탈퇴는 같은 가족의 연결을 모두 삭제합니다.

### family_codes

- 사용자별 고정 코드입니다.
- 코드 형식은 `MSH-0000` 패턴입니다.
- `code`는 unique이며 생성 시 충돌을 재시도합니다.

### family_code_usages

- 누가 어떤 가족 코드를 입력해 어떤 가족과 매칭됐는지 기록합니다.
- `(family_code_id, requester_user_id)`는 unique입니다.

---

## 부모 프로필 테이블 구현 메모

### parent_profiles

- `user_id`는 unique입니다. 부모 사용자 1명은 부모님 상세 프로필을 최대 1개만 가집니다.
- `status`는 `draft`, `completed`를 사용합니다.
- 단계별 저장을 위해 `current_step`, `completion_percent`를 저장합니다.
- 하루 이동 성향은 `walking_pace`에 `slow`, `normal`, `fast` 중 하나로 저장합니다.
- 음식 취향은 `food_preference`에 `korean`, `familiar`, `adventurous` 중 하나로 저장합니다.
- 이동 도움 필요 여부는 `needs_mobility_assistance`에 저장합니다. 코스 추천 시 무장애 정보 가중치로 사용합니다.
- 추천용 여행 MBTI 현재값 1개를 `personality_type`에 저장하며, 프로필을 다시 완료하면 새 결과로 덮어씁니다.
- 여행 MBTI 타입과 가중치 계산 정책은 `docs/policy/parent-travel-mbti.md`를 기준으로 합니다.
- 유형명과 결과 문구는 서버 정책에서 관리하므로 별도 유형 마스터, 진단 이력, 점수 테이블을 두지 않습니다.
- 여행 생성 시점의 프로필 입력과 MBTI 결과는 `trips.recommendation_snapshot`에 보관합니다.

### parent_profile_themes

- `parent_profile_id`, `theme_code` 조합은 unique입니다.
- `theme_code`는 `nature_scenery`, `history_culture`, `shopping`, `activity`, `culture_life`, `landmark`, `experience` 중 하나입니다.
- 현재 구현은 별도 마스터 테이블 FK 대신 enum code를 직접 저장합니다.
- 여행 테마 최소 1개, 최대 3개 제약은 프로필 완료 시 서비스에서 검증합니다.

---

## 여행 테이블 구현 메모

### trips

- `family_id`는 여행을 소유한 가족입니다.
- `created_by_user_id`는 여행을 생성한 자녀 사용자입니다.
- MVP 구현은 `travel_destinations` 마스터 테이블 FK 대신 `destination_code` enum 문자열을 저장합니다.
- `destination_code`는 `daegu`, `gangneung_sokcho`, `gyeongju`, `busan`, `yeosu`, `incheon`, `jeonju`, `jeju`, `seoul`, `suwon_yongin`, `tongyeong_geoje_namhae`, `pohang_andong` 중 하나입니다.
- 미래 여행의 생성 직후 `status`는 `planning`이며, 시작일이 오늘이면 생성 응답부터 `in_progress`입니다.
- 서울 날짜 기준 시작일부터 준비 여부와 관계없이 `in_progress`, 종료일 다음 날부터 `completed`로 동기화합니다. `archived`는 날짜로 변경하지 않습니다.
- 여행 생성 시 `recommendation_snapshot`에 부모 프로필 추천 입력값을 JSON 문자열로 저장합니다.
  현재 스냅샷에는 정책 버전, 도시/날짜, 부모별 `walkingPace`, `needsMobilityAssistance`, `travelThemes`, `foodPreference`, `personalityType`을 포함합니다.
- 같은 가족에서 날짜가 겹치는 여행은 서비스에서 생성 거부합니다.
- 여행 기간 상한은 두지 않습니다. 시작일은 오늘 또는 이후여야 하고, 종료일은 시작일과 같거나 이후여야 합니다.
- 여행을 만든 자녀는 `planning`, `ready` 상태에서 제목, 도시, 날짜, 참여 부모를 수정할 수 있습니다.
- `in_progress`에서는 제목과 도시를 고정하고, 서울 기준 오늘을 포함하는 기간과 참여 부모만 수정할 수 있습니다. `completed`, `archived`에서는 수정할 수 없습니다.
- 제목만 수정하면 `recommendation_snapshot`을 유지합니다. 도시, 날짜 또는 참여 부모가 바뀌면 현재 부모 프로필 기준으로 스냅샷을 다시 저장합니다. 준비 중에는 `planning`, 여행 중에는 날짜 동기화 후 `in_progress` 상태가 됩니다.

### trip_participants

- `trip_id`, `user_id` 조합은 unique입니다.
- 여행 생성 시 생성 자녀와 선택한 부모를 참여자로 저장합니다.
- 부모는 최대 2명까지 선택할 수 있습니다.
- 참여 부모 변경 시 생성 자녀는 유지하고 선택한 부모 목록으로 기존 참여자를 교체하며, 기존 여행 10계명과 모든 서명을 함께 삭제합니다.
- 대표 자녀 탈퇴로 가족 연결이 끊겨도 완료 여행 참여 여부를 확인할 수 있도록 과거 참여자 row는 유지합니다.
- 활성 사용자는 현재 가족의 완료 여행과 자신이 참여했던 완료 여행을 기록 탭에서 조회할 수 있습니다.

### trip_days

- 여행 시작일과 종료일을 기준으로 날짜 수만큼 `trip_days` row를 생성합니다.
- `trip_id`, `day_number` 조합은 unique입니다.
- Tmap 경로 최적화 결과를 일자 단위 캐시로 저장합니다.
- `route_provider`는 현재 `tmap`을 사용합니다.
- `route_total_distance_m`, `route_total_duration_seconds`는 Tmap 응답의 `totalDistance`, `totalTime`을 저장합니다.
- `route_polyline`은 지도 표시용 좌표 목록 JSON 문자열입니다.
- `route_source_payload`는 선택된 시작/도착/방문 순서와 Tmap 응답 properties 일부를 저장합니다.
- `route_optimized_at`은 경로 계산 시간입니다.
- 코스 저장이나 추천 코스 재생성으로 방문지 구성이 바뀌면 경로 캐시를 비웁니다.
- 여행 날짜 변경 시 기존 `trip_stops`를 먼저 삭제한 뒤 `trip_days`를 새 기간에 맞춰 재생성합니다.

### trip_stops

- `trip_day_id`, `sort_order` 조합은 unique입니다.
- 코스 저장, 추천 재생성, 경로 최적화는 여행을 만든 자녀만 `planning`, `ready`, `in_progress` 상태에서 할 수 있습니다.
- 코스 저장 API는 요청 배열 순서대로 `sort_order`를 1부터 다시 부여하고 기존 방문지를 전체 덮어씁니다.
- Tmap 경로 최적화 API는 최적화 결과 기준으로 `sort_order`를 다시 부여하고, `arrival_time`과 비어 있던 `dwell_minutes` 기본값을 갱신합니다.
- 현재 구현은 `places` 마스터 FK 없이 장소명, 카테고리, 주소, 좌표, 대표 이미지, 소개, 외부 장소 ID 등을 스냅샷으로 저장합니다.
- `source_provider`는 `tour_api`, `tmap`, `kakao_map`, `public_data`, `local_excel`, `internal` 중 하나입니다.
- `stop_type`은 `sightseeing`, `meal`, `rest`, `cafe` 중 하나입니다.
- `recommendation_tags`, `source_payload`는 JSON 문자열로 저장합니다.
- 장소 마스터 캐시와 방문지 간 세그먼트 단위 상세 테이블은 후속 작업입니다. 현재 경로는 `trip_days` 일자 단위 캐시에 저장합니다.

### support_facilities

- `facility_type`, `provider`, `source_id` 조합은 unique입니다.
- 현재 DB에 저장하는 `facility_type`은 `restroom`입니다.
- 병원과 약국은 Tmap POI에서 실시간 조회하므로 `support_facilities`에 저장하지 않습니다.
- 공중화장실 원천 `provider`는 `local_excel`이고 `source_id`는 `개방자치단체코드:관리번호`입니다.
- 원천 CSV에 좌표가 없어 Tmap으로 WGS84 좌표를 한 번 변환해 저장합니다.
- `raw_data`는 원본 CSV row와 좌표 변환 제공자를 JSON 문자열로 저장합니다.
- `(facility_type, latitude, longitude)` 인덱스로 현재 위치 주변 bounding box 후보를 조회합니다.
- 실제 거리와 정렬은 서비스에서 Haversine 공식으로 계산합니다.

---

## 여행 10계명 테이블 구현 메모

### pledge_templates

- 서버 시작 시 와이어프레임 기준 기본 후보 문구 중 DB에 없는 문구만 추가합니다.
- `is_active=true`인 후보만 무작위 후보 조회에 사용합니다.
- 후보 조회 결과 자체는 저장하지 않습니다.

### trip_pledges

- `trip_id`는 unique이며 여행당 확정본을 하나만 저장합니다.
- 상태는 `draft`, `reviewed`, `signature_requested`, `completed` 순서로 진행합니다.
- 자녀가 서명하면 `signature_requested`, 첫 참여 부모가 서명하면 `completed`로 변경합니다.
- `completed_at`은 최초 완료 시각으로 유지하며 이후 다른 부모의 추가 서명으로 변경하지 않습니다.
- 10계명과 서명 시각은 DB `timestamp(6)` 정밀도에 맞춰 마이크로초 단위로 저장하여 저장 직후 응답과 재조회 응답을 동일하게 유지합니다.
- 완성된 이미지와 PDF는 저장하지 않으며 조회 시 HTML 템플릿과 현재 DB 데이터를 합성하므로 렌더링/PDF URL 컬럼을 두지 않습니다.
- PDF 공유 조건은 자녀 서명과 여행 참여 부모 최소 1명 서명 완료입니다.
- 참여 부모 ID 집합이 바뀌면 `pledge_signatures`, `pledge_items`, `trip_pledges` 순서로 모두 삭제하고 새 참여자 구성에서 다시 작성합니다.

### pledge_items

- `trip_pledge_id`, `sort_order` 조합은 unique입니다.
- 여행별 항목은 정확히 10개이며 요청 배열 순서대로 1~10을 저장합니다.
- 원본 템플릿을 수정 없이 사용한 경우에만 `is_from_template=true`입니다.
- 직접 작성한 항목은 `pledge_template_id`가 null일 수 있습니다.

### pledge_signatures

- `trip_pledge_id`, `user_id` 조합은 unique이며 제출한 서명은 덮어쓰지 않습니다.
- `signature_image_data`는 클라이언트가 전송한 PNG Base64를 디코딩한 바이트이며 PostgreSQL `bytea`로 저장합니다.
- `signature_mime_type`은 현재 `image/png`로 고정합니다.
- 디코딩된 이미지의 최대 허용 크기는 512KB입니다.
- 모든 여행 참여자에게 현재까지 저장된 전체 서명을 동일하게 제공합니다.

---

## 여행 피드백 테이블 구현 메모

### trip_feedback_requests

- `trip_id`, `parent_user_id` 조합은 unique입니다.
- 여행 생성 자녀가 마지막 날부터 아직 제출하지 않은 참여 부모 전체에게 요청합니다.
- 같은 API를 다시 호출해도 기존 부모별 요청 시각을 유지합니다.

### trip_feedbacks

- `trip_id`, `parent_user_id` 조합은 unique이며 부모별 한 번만 제출합니다.
- `overall_rating`은 `0.0`부터 `5.0`까지 `0.5` 단위로 서비스에서 검증합니다.
- `body_condition`은 `comfortable`, `slightly_tired`, `very_tired` 중 하나입니다.
- `best_trip_stop_id`는 제출 시 해당 여행 방문지인지 검증하지만 FK는 두지 않습니다.
- `best_place_name_snapshot`에 제출 당시 장소명을 저장해 이후 코스 변경과 관계없이 표시합니다.
- `submitted_at`은 DB `timestamp(6)` 정밀도에 맞춰 마이크로초 단위로 저장합니다.
- 여행 기간 또는 참여 부모 구성이 바뀌면 관련 요청과 피드백을 모두 삭제합니다.

### trip_feedback_tags

- `TripFeedback.tags`의 `@ElementCollection` 테이블입니다.
- `trip_feedback_id`, `tag` 조합은 unique입니다.
- 좋았던 점 6개와 개선할 점 4개 중 실제로 선택한 값만 저장합니다.
- API enum과 화면 문구 매핑은 `docs/policy/trip-feedback.md`를 따릅니다.

---

## 효도 리포트 테이블 구현 메모

### filial_reports

- `trip_id`는 unique이며 여행별 리포트는 한 건만 저장합니다.
- 마지막 참여 부모의 피드백 제출과 동시에 생성합니다.
- `average_rating`은 부모별 전체 만족도 평균을 소수점 첫째 자리까지 저장합니다.
- `total_place_count`는 현재 방문지 수, `total_distance_km`는 값이 있는 일자별 Tmap 경로 거리의 합입니다.
- `cover_image_url`은 이미지가 있는 부모 베스트 장소를 우선하고, 없으면 첫 번째 방문지 이미지를 사용합니다.
- 코스가 수정될 수 있는 마지막 날에는 생성·조회 API 호출 시 코스 기반 집계값을 다시 맞추고 최초 `generated_at`은 유지합니다.
- 산식이 정해지지 않은 점수, 수상 문구, 요약, 걸음 수, 공유 이미지 URL은 nullable 상태로 유지합니다.
- 여행 기간 또는 참여 부모 구성이 바뀌면 피드백과 함께 기존 리포트를 삭제합니다.
- `filial_report_stop_summaries`는 장소별 편안/주의 배지와 요약 기준이 정해진 뒤 Entity로 구현합니다.
- 기록 탭 통계는 별도 테이블 없이 `completed` 여행의 코스, Tmap 경로, 제출된 피드백을 조회 시 집계합니다.

---

## 여행 AI 챗봇 테이블 구현 메모

### chat_sessions

- `user_id`, `trip_id`, `scope`를 기준으로 종료되지 않은 최신 세션을 조회합니다.
- 현재 구현은 여행 모드 전용이므로 `trip_id`가 필수이며 `scope`는 `travel_mode`입니다.
- 첫 질문을 보낼 때 사용자별·여행별 세션을 자동 생성하고, 이후 질문마다 최신 코스로 `context_snapshot`을 갱신합니다.
- `system_prompt_version`과 `model_name`은 해당 세션에 적용한 프롬프트·모델을 추적합니다.
- `context_snapshot`은 여행과 코스 문맥을 직렬화한 JSON 문자열이며 DB에는 `text`로 저장합니다.
- 부모 프로필과 여행 MBTI는 현재 AI 문맥에 넣지 않으므로 `personalization_enabled=false`입니다.

### chat_messages

- `chat_session_id`에 속한 사용자 질문과 AI 답변을 시간순으로 저장합니다.
- `sender`는 `user`, `assistant` 중 하나입니다.
- AI 답변의 `metadata`는 OpenAI response ID, 모델명, token 사용량을 직렬화한 JSON 문자열이며 DB에는 `text`로 저장합니다.
- OpenAI 호출이 실패하면 같은 트랜잭션에서 생성한 세션과 사용자 질문도 저장하지 않습니다.

---

## 로컬 DB

로컬 profile은 H2 인메모리 DB를 사용합니다.

- JDBC URL: `jdbc:h2:mem:msdragon`
- username: `sa`
- H2 Console path: `/h2-console`

---

## Entity 추가 시 기록할 항목

- 테이블명
- 컬럼명, 타입, nullable 여부
- 인덱스와 unique 제약
- 연관관계
- 공통 timestamp/base entity 적용 여부
