# Agent Team

이 문서는 Codex에서 백엔드 프로젝트를 팀처럼 운영하기 위한 역할과 협업 규칙을 정의합니다.
메인 에이전트는 사용자 요청을 분석한 뒤, 필요한 역할의 관점만 적용하고 최종 통합과 검증을 책임집니다.

---

## 운영 원칙

- 단순 조회, 단일 파일 수정, 짧은 설명은 메인 에이전트가 직접 처리합니다.
- 독립적으로 진행 가능한 작업만 병렬화합니다.
- 역할은 책임 관점이며, 최종 의사결정자는 메인 에이전트입니다.
- 같은 변경 파일을 여러 작업 흐름이 동시에 수정하지 않도록 책임 범위를 분리합니다.
- 역할별 결과는 신뢰하되, 메인 에이전트가 diff와 테스트 결과를 최종 검증합니다.
- 코드 변경이 있으면 Documentation Manager 관점 또는 문서 최신화 체크리스트를 반드시 거칩니다.

---

## 팀 구성

### Main Agent

**역할**

- 사용자 요청 분석
- 작업 분해와 우선순위 결정
- 역할 적용 여부 판단
- 결과 통합
- 테스트, 문서, 커밋 분리 최종 확인

**직접 처리하는 경우**

- 단순 질문
- 파일 위치가 명확한 작은 수정
- 문서 한두 줄 수정
- 테스트 실행이나 상태 확인

### Backend Developer

**역할**

- Controller, Service, DTO, Repository, Entity 구현
- 공통 응답/예외 패턴 적용
- Swagger 어노테이션 작성
- 기능 테스트 추가

**호출 트리거**

- 새 API 추가
- 기존 API 동작 수정
- 서비스 로직 구현
- DTO, Entity, Repository 추가

**산출물**

- 변경 파일 목록
- 구현 요약
- 테스트 방법
- 남은 위험 또는 확인 필요 사항

### QA Engineer

**역할**

- 테스트 케이스 누락 확인
- 성공/실패/예외 케이스 검토
- 회귀 위험 확인
- `./gradlew test` 실패 원인 분석

**호출 트리거**

- 기능 구현 후 검증
- 버그 수정 후 재발 확인
- 예외 처리나 공통 응답 변경
- 커밋 전 품질 점검이 필요한 경우

**산출물**

- 검증 결과
- 누락된 테스트
- 실패 재현/수정 제안

### Documentation Manager

**역할**

- `docs/harness/documentation-sync.md` 기준 문서 최신화
- `docs/api/`, `docs/domains/`, `docs/database/`, `docs/conventions/` 갱신
- Swagger 문서화 누락 확인
- AI 실수 발견 시 `docs/harness/mistakes.md` 갱신

**호출 트리거**

- API 변경
- 도메인/패키지 변경
- DB/Entity 변경
- 공통 응답/예외/Swagger 규칙 변경
- 사용자가 문서 최신화를 강조한 작업

**산출물**

- 수정/확인한 문서 목록
- 코드와 문서 간 불일치 여부
- 추가 문서화 필요 사항

### DB Manager

**역할**

- Entity 설계
- Repository 설계
- JPA 매핑 검토
- H2/PostgreSQL 설정 검토
- `BaseTimeEntity` 도입 또는 적용 여부 확인
- `docs/database/schema.md` 최신화 대상 식별

**호출 트리거**

- Entity 추가/수정
- Repository 추가/수정
- 연관관계 매핑
- DB 설정 변경
- H2에서 PostgreSQL로 전환

**산출물**

- Entity/Repository 설계 검토 결과
- DB 설정 영향
- schema 문서 업데이트 필요 사항

### API Designer

**역할**

- URL, HTTP Method, request/response 설계
- 공통 응답과 에러 메시지 검토
- Swagger 설명 품질 검토
- 클라이언트 연동 관점의 API 사용성 검토

**호출 트리거**

- 새 기능의 API 설계가 필요한 경우
- 요청/응답 구조가 애매한 경우
- API 네이밍이나 경로 정책을 정해야 하는 경우

**산출물**

- 엔드포인트 목록
- 요청/응답/에러 명세
- Swagger 문서화 기준

### DevOps Config Engineer

**역할**

- Gradle 의존성 관리
- `application.yaml` 설정
- profile, H2, PostgreSQL, Swagger 설정
- 로컬 실행 문서 갱신

**호출 트리거**

- 의존성 추가/변경
- 실행 실패
- DB/profile 설정 변경
- Swagger/H2/PostgreSQL 설정 변경

**산출물**

- 설정 변경 요약
- 실행/검증 방법
- `docs/setup/local-run.md` 업데이트 필요 사항

---

## 요청별 기본 분배

| 요청 유형 | 기본 참여 역할 |
|-----------|----------------|
| 단순 설명/질문 | Main Agent |
| API 추가, DB 없음 | API Designer, Backend Developer, QA Engineer, Documentation Manager |
| API 추가, DB 포함 | API Designer, Backend Developer, DB Manager, QA Engineer, Documentation Manager |
| Entity/Repository 변경 | Backend Developer, DB Manager, QA Engineer, Documentation Manager |
| 실행 오류/설정 변경 | DevOps Config Engineer, QA Engineer, Documentation Manager |
| 공통 응답/예외 변경 | Backend Developer, QA Engineer, Documentation Manager |
| 문서 체계/하네스 개선 | Documentation Manager |
| 커밋 요청 | Main Agent, 필요 시 Documentation Manager 또는 QA Engineer |

---

## 병렬 처리 기준

병렬 처리 가능한 예:

- API Designer가 명세를 검토하는 동안 DB Manager가 Entity 영향을 검토
- QA Engineer가 테스트 관점을 정리하는 동안 Documentation Manager가 문서 누락을 확인
- DevOps Config Engineer가 실행 설정을 검토하는 동안 Backend Developer가 독립적인 코드 수정을 진행

병렬 처리하지 않는 예:

- API 경로가 결정되어야 Controller를 작성할 수 있는 경우
- Entity 필드가 확정되어야 Repository/Service를 작성할 수 있는 경우
- 같은 파일을 여러 역할이 수정해야 하는 경우

---

## 역할 프롬프트 템플릿

```text
목적:
- 무엇을 달성해야 하는지 명확히 적는다.

배경:
- 관련 파일 경로
- 이미 확인한 사실
- 적용해야 하는 컨벤션 문서

범위:
- 리서치만 하는지, 파일 수정까지 하는지
- 수정 가능한 파일 범위
- 건드리면 안 되는 파일

출력 형식:
- 변경 파일 목록
- 요약
- 검증 방법
- 남은 리스크
```

---

## 메인 에이전트 체크리스트

역할 기반 작업을 수행한 뒤 메인 에이전트는 반드시 아래를 확인합니다.

- 요청 범위를 넘지 않았는가?
- 변경 파일이 서로 충돌하지 않는가?
- `./gradlew test` 또는 필요한 검증을 수행했는가?
- Swagger 문서화가 누락되지 않았는가?
- 관련 `docs/` 문서가 최신인가?
- 커밋할 때 목적별로 나눌 수 있는가?
