# 도메인 대분류

이 문서는 Codex가 커밋 스코프를 결정할 때 참조합니다.
**커밋 전마다 에이전트가 `src/` 패키지 구조를 스캔하여 이 목록을 최신 상태로 유지합니다.**

---

## 현재 등록된 도메인

| 스코프 | 패키지 경로 | 설명 |
|--------|------------|------|
| `root` | `com.msdragon.backend` | 애플리케이션 진입점 |
| `common` | `com.msdragon.backend.common` | 공통 응답, 예외, advice, config, 엔티티 기반 클래스가 추가될 때 사용 |
| `health` | `com.msdragon.backend.health` | health 체크 API (`controller/service/dto`) |

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
