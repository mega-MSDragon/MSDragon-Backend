# Profile Domain

Profile 도메인은 마이페이지의 내 기본 프로필 조회/수정을 담당합니다.

---

## 책임

- 로그인 사용자의 기본 프로필을 조회합니다.
- 로그인 사용자의 이름, 연령대, 성별을 수정합니다.
- 역할별 허용 연령대 정책은 auth 도메인의 `UserProfilePolicy`를 재사용합니다.
- 연결 가족 프로필 조회는 family 도메인의 가족 조회 API에서 제공합니다.

---

## 패키지 구조

```text
profile
├── controller
├── dto
└── service
```

---

## 관련 테이블

- `users`

---

## 관련 API

- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`

---

## 후속 범위

- 연결된 가족 구성원 프로필 수정 권한 정책
- 알림 설정
- 약관/버전 정보
- MBTI 상세 조회와 재검사 플로우
