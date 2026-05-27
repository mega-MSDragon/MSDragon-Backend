# 문서 최신화 트리거

이 문서는 코드 변경과 문서 변경이 어긋나지 않도록, Codex가 매 작업마다 확인해야 하는 문서 최신화 절차를 정의합니다.

---

## 기본 원칙

- 코드를 바꿨다면 관련 문서도 같은 작업 흐름 안에서 최신화합니다.
- 문서가 필요 없는 변경이라고 판단하더라도, 왜 필요 없는지 커밋 전 스스로 확인합니다.
- 문서는 코드의 부속물이 아니라 다음 작업자의 컨텍스트입니다.
- 오래된 문서는 없는 문서보다 위험하므로, 불확실한 내용은 확정처럼 쓰지 않습니다.

---

## 트리거

### 1. 작업 시작 전

작업을 시작하기 전에 아래 문서를 확인합니다.

- `docs/harness/mistakes.md`: 이전 AI 실수와 재발 방지 규칙
- `docs/harness/documentation-sync.md`: 문서 최신화 절차
- `docs/harness/agent-team.md`: 팀형 작업 분배 기준
- `docs/conventions/commit.md`: 커밋 분리와 메시지 규칙
- 변경 대상에 해당하는 `docs/api/`, `docs/domains/`, `docs/database/`, `docs/conventions/` 문서

### 2. 파일 수정 전

수정하려는 코드가 아래 중 하나에 해당하면 관련 문서 업데이트 대상을 미리 정합니다.

| 변경 유형 | 확인/수정할 문서 |
|-----------|------------------|
| 새 API 추가/수정 | `docs/api/{도메인}.md`, Swagger 어노테이션 |
| 새 도메인/기능 추가 | `docs/domains/{도메인}.md`, `docs/domains.md` |
| Entity/Repository/DB 설정 변경 | `docs/database/schema.md` |
| 공통 응답/예외 변경 | `docs/conventions/api-response.md`, `docs/conventions/exception.md` |
| 패키지 구조 변경 | `docs/conventions/package.md`, `docs/domains.md` |
| 실행/환경 설정 변경 | `docs/setup/local-run.md` |
| AI 실수 발견 또는 사용자의 누락/오판 지적 | `docs/harness/mistakes.md` |
| Codex 작업 방식 변경 | `docs/harness/agent-team.md`, `AGENTS.md` |
| 커밋/작업 규칙 변경 | `docs/conventions/commit.md`, `.codex/commands/commit.md` |

### 3. 파일 수정 후

코드 변경 후 아래를 확인합니다.

- Swagger 설명이 필요한 API에 `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@Schema`가 있는가?
- API 요청/응답 구조가 `docs/api/` 문서와 일치하는가?
- 공통 응답/예외 포맷이 컨벤션 문서와 일치하는가?
- Entity 필드와 DB 문서가 일치하는가?
- 새 패키지가 `docs/domains.md`에 반영되었는가?

### 4. 커밋 전

커밋 전 반드시 아래 명령과 문서 확인을 수행합니다.

```bash
git status --short
git diff --stat
git diff
```

확인할 것:

- 변경사항을 목적별로 분리할 수 있는가?
- 각 목적별 변경에 필요한 문서가 같이 수정되었는가?
- 문서만 바뀌었는데 코드와 충돌하는 오래된 설명은 없는가?
- 코드만 바뀌었는데 문서가 누락되지는 않았는가?

---

## 문서 작성 단위

백엔드 문서는 화면 단위가 아니라 기능/도메인 단위로 관리합니다.

- API 문서: `docs/api/{도메인}.md`
- 도메인 문서: `docs/domains/{도메인}.md`
- DB 문서: `docs/database/schema.md`
- 컨벤션 문서: `docs/conventions/*.md`
- 환경/실행 문서: `docs/setup/*.md`
- 하네스 문서: `docs/harness/*.md`

---

## 커밋 분리와 문서

- 기능 변경과 문서 최신화는 같은 목적이면 같은 커밋에 포함합니다.
- 관계없는 문서 규칙 변경은 별도 `docs` 커밋으로 분리합니다.
- 예: 회원 API에 Swagger 설명을 추가하면서 `docs/api/member.md`를 갱신하는 것은 같은 커밋에 포함합니다.
- 예: Swagger 설명 추가와 AI 실수 기록 문서 추가는 목적이 다르므로 별도 커밋으로 분리합니다.
