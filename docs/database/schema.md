# Database Schema

DB 스키마와 공통 엔티티 규칙을 기록합니다.

---

## 현재 상태

아직 도메인 Entity가 정의되지 않았습니다.

---

## 공통 엔티티 기반

생성/수정 시간 추적이 필요한 JPA Entity에는 `BaseTimeEntity` 형태의 공통 기반 클래스를 도입하는 것을 기본 기준으로 합니다.
아직 코드가 도입되지 않았다면 Entity 추가 시 함께 검토합니다.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `createdAt` | `LocalDateTime` | true | 생성 시간, JPA Auditing으로 기록 |
| `updatedAt` | `LocalDateTime` | true | 수정 시간, JPA Auditing으로 기록 |

---

## 로컬 DB

현재 `build.gradle.kts`에는 H2와 PostgreSQL driver 의존성이 포함되어 있습니다.
실제 datasource/profile/H2 Console 설정이 추가되면 아래 항목을 확정합니다.

- JDBC URL
- username/password
- H2 Console 경로
- PostgreSQL 전환 방법

---

## Entity 추가 시 기록할 항목

- 테이블명
- 컬럼명, 타입, nullable 여부
- 인덱스와 unique 제약
- 연관관계
- 공통 timestamp/base entity 적용 여부
