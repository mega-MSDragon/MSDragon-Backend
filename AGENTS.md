# AGENTS.md

이 파일은 Codex가 이 저장소에서 작업할 때 따르는 행동 지침과 프로젝트 가이드입니다.
추가 문서는 `docs/` 디렉터리를 참고하세요.

> **하네스 엔지니어링**: 이 프로젝트의 최우선 운영 원칙입니다. 에이전트는 작업 중 발견한 오류, 보완점, 패턴을 스스로 `docs/`에 기록하여 다음 작업자가 더 좋은 컨텍스트로 시작할 수 있도록 프로젝트 지식을 성장시킵니다.

---

## 행동 원칙

### 1. 코딩 전에 먼저 생각하기

가정하지 말고, 혼란을 숨기지 말고, 트레이드오프를 드러냅니다.

- 구현 전에 가정을 명시합니다.
- 해석이 여러 가지라면 선택지를 밝히고, 위험한 선택은 질문합니다.
- 더 단순한 접근이 있으면 먼저 제안합니다.
- 불분명한 요구사항으로 인해 되돌리기 어려운 변경이 생길 수 있으면 멈추고 확인합니다.

### 2. 단순함 우선

문제를 해결하는 최소한의 코드를 작성합니다.

- 요청된 것 이상의 기능을 추가하지 않습니다.
- 단일 용도 코드에 추상화를 끼워 넣지 않습니다.
- 추측성 확장성이나 설정 가능성을 만들지 않습니다.
- 기존 스타일과 패턴을 우선합니다.

### 3. 외과적 변경

반드시 건드려야 하는 것만 건드립니다.

- 관련 없는 리팩터링, 포맷팅, 주석 정리는 하지 않습니다.
- 내가 만든 변경으로 인해 불필요해진 import, 변수, 함수만 정리합니다.
- 기존에 존재하던 죽은 코드는 요청받지 않는 한 삭제하지 않습니다.
- 변경된 모든 줄은 사용자의 요청으로 직접 추적 가능해야 합니다.

### 4. 목표 주도 실행

작업을 검증 가능한 목표로 바꿉니다.

- 버그 수정은 가능하면 재현 테스트를 먼저 만들고 통과시킵니다.
- 리팩터링은 변경 전후 테스트 통과를 확인합니다.
- 여러 단계 작업은 짧은 계획과 검증 방법을 먼저 정리합니다.

---

## 프로젝트 개요

`MSDragon-Backend`는 Kotlin + Spring Boot 4 기반의 백엔드 프로젝트입니다.

- **Group**: `com.msdragon`
- **Root package**: `com.msdragon.backend`
- **Java toolchain**: 21
- **Kotlin**: 2.2.21
- **Spring Boot**: 4.0.6
- **Build tool**: Gradle Kotlin DSL

## 명령어

```bash
# 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 전체 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.msdragon.backend.BackendApplicationTests"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.msdragon.backend.BackendApplicationTests.contextLoads"
```

## 아키텍처

`docs/conventions/package.md`의 기능별 레이어드 패키지 구조를 따릅니다. 기능 패키지는 `controller`, `dto`, `service`, `entity`, `repository`로 나누고 공통 코드는 `common`에 둡니다.

- 진입점: `src/main/kotlin/com/msdragon/backend/BackendApplication.kt`
- 설정: `src/main/resources/application.yaml`, profile별 `application-local.yaml` / `application-prod.yaml`
- 기능 패키지: `auth`, `chat`, `common`, `family`, `feedback`, `health`, `home`, `parentprofile`, `pledge`, `profile`, `report`, `supportfacility`, `trip`
- 공통 응답/예외: `common/response/ApiResponse.kt`, `common/exception/ControllerExceptionAdvice.kt`
- 인증: `auth/support/AuthInterceptor.kt`가 `/api/v1/**`를 검사하고 `/api/v1/auth/**`는 제외합니다
- 정적 리소스: `src/main/resources/static/policies/`의 개인정보처리방침·이용약관 HTML은 인증 없이 웹뷰로 제공됩니다
- PDF 템플릿: `src/main/resources/templates/pledge/`, 폰트: `src/main/resources/fonts/`
- DB: 배포는 PostgreSQL, 테스트는 H2. Flyway 없이 `ddl-auto=update`를 사용합니다
- 주요 스타터: Spring Web MVC, Spring Data JPA, Validation, springdoc

패키지를 추가하거나 바꾸면 `docs/domains.md`와 해당 `docs/domains/{도메인}.md`를 함께 갱신합니다.

## Kotlin/JPA 특이사항

- `allOpen` 플러그인이 `@Entity`, `@MappedSuperclass`, `@Embeddable`에 적용되어 있으므로 수동으로 `open`을 붙이지 않아도 됩니다.
- `-Xjsr305=strict`: JSR-305 어노테이션에 Kotlin null-safety를 강제합니다.
- `-Xannotation-default-target=param-property`: 생성자 파라미터 어노테이션이 파라미터와 backing property 양쪽에 적용됩니다.

---

## 문서 구조

관련 작업 전 해당 문서를 먼저 확인합니다. 새로운 규칙이나 패턴이 발견되면 즉시 해당 문서에 추가합니다.

```text
docs/
├── api/                     # API 스펙 및 엔드포인트 문서
├── conventions/
│   ├── api-response.md      # 공통 응답 컨벤션
│   ├── commit.md            # 커밋 메시지 컨벤션
│   ├── exception.md         # 예외 처리 컨벤션
│   ├── package.md           # 패키지 구조 컨벤션
│   └── swagger.md           # Swagger 문서화 컨벤션
├── database/
│   └── schema.md            # DB 스키마 및 공통 엔티티 문서
├── domains/                 # 기능/도메인별 문서
├── harness/
│   ├── agent-team.md        # Codex 작업 역할과 분배 기준
│   ├── documentation-sync.md # 문서 최신화 트리거
│   └── mistakes.md          # AI 실수 재발 방지 기록
├── domains.md               # 패키지 대분류 목록
├── setup/
│   └── local-run.md         # 로컬 실행 방법
└── adr/                     # Architecture Decision Records

.codex/
├── commands/
│   └── commit.md            # Codex 커밋 작업 절차
└── hooks/
    └── post_edit_lint.sh    # 편집 후 기본 검증 훅
```

## 하네스 엔지니어링 실천 규칙

- **능동적 문서화**: 작업 중 새로운 패턴, 규칙, 특이사항을 발견하면 적절한 `docs/` 문서에 즉시 기록합니다.
- **문서 최신화 트리거 준수**: 작업 시작 전, 파일 수정 후, 커밋 전 `docs/harness/documentation-sync.md`의 체크리스트를 따릅니다.
- **팀형 작업 운영**: 복합 작업은 `docs/harness/agent-team.md` 기준으로 역할을 나누되, 단순 작업은 직접 처리합니다.
- **도메인 동기화**: 패키지 구조가 변경될 때마다 `docs/domains.md`를 업데이트합니다.
- **컨벤션 준수**: 커밋 전 반드시 `docs/conventions/commit.md`를 참조합니다.
- **오류 기록**: 반복되는 실수나 엣지 케이스를 발견하면 `docs/harness/mistakes.md`에 기록합니다.
- **API 문서화 기본 포함**: 컨트롤러/API 작업에는 사용자가 따로 말하지 않아도 Swagger `@Tag`, `@Operation`, `@ApiResponses`, 필요한 `@Parameter`/`@Schema`를 함께 작성합니다.
- **Codex 명령 활용**: 커밋 요청은 `.codex/commands/commit.md`의 절차를 우선 적용합니다.
