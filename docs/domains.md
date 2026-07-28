# 도메인 대분류

이 문서는 Codex가 커밋 스코프를 결정할 때 참조합니다.
**커밋 전마다 에이전트가 `src/` 패키지 구조를 스캔하여 이 목록을 최신 상태로 유지합니다.**

---

## 현재 등록된 도메인

| 스코프 | 패키지 경로 | 설명 |
|--------|------------|------|
| `root` | `com.msdragon.backend` | 애플리케이션 진입점 |
| `common` | `com.msdragon.backend.common` | 공통 응답, 예외, advice, config, 엔티티 기반 클래스가 추가될 때 사용 |
| `auth` | `com.msdragon.backend.auth` | 카카오/애플 소셜 로그인, 회원가입 완료, 토큰 재발급 |
| `family` | `com.msdragon.backend.family` | 가족 코드 발급, 가족 매칭, 가족 구성원 조회 |
| `feedback` | `com.msdragon.backend.feedback` | 마지막 날 부모 평가 요청, 부모별 여행 피드백 제출과 현황 조회 |
| `health` | `com.msdragon.backend.health` | health 체크 API (`controller/service/dto`) |
| `profile` | `com.msdragon.backend.profile` | 마이페이지 내 프로필 조회와 수정 |
| `parentprofile` | `com.msdragon.backend.parentprofile` | 부모님 상세 프로필 작성/수정/조회와 추천용 여행 MBTI 현재값 저장 |
| `pledge` | `com.msdragon.backend.pledge` | 여행 10계명 후보 제공과 여행별 확정 문구 저장 |
| `report` | `com.msdragon.backend.report` | 부모 피드백 기반 효도 리포트 자동 생성과 조회 |
| `trip` | `com.msdragon.backend.trip` | 여행 생성·조회, 날짜 기반 여행 모드, TourAPI 추천 코스, Tmap 경로 최적화 |

---

## 관리 규칙

- `src/main/kotlin/com/msdragon/backend/` 하위에 새 패키지가 생기면 이 목록에 추가합니다.
- 패키지가 삭제되면 이 목록에서도 제거합니다.
- 스코프 이름은 패키지명과 일치시키되, 너무 길면 축약합니다.
- 도메인이 추가/삭제될 때는 변경 이유를 간략히 기록합니다.
- 도메인별 상세 문서는 `docs/domains/{domain}.md`에 작성합니다.

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|-----------|
| 2026-05-27 | Codex 하네스 초기 구조 생성 (`root`, `common` 기준 문서화) |
| 2026-05-27 | health 체크 API 도메인 추가 |
| 2026-05-27 | Docker Compose 기반 배포 구성 추가 |
| 2026-06-29 | auth 도메인 추가 |
| 2026-06-30 | family 도메인 추가 |
| 2026-06-30 | profile 도메인 추가 |
| 2026-07-01 | parentprofile 도메인 추가 |
| 2026-07-06 | trip 도메인 추가 |
| 2026-07-15 | pledge 도메인 추가 |
| 2026-07-26 | feedback 도메인 추가 |
| 2026-07-28 | report 도메인 추가 |
