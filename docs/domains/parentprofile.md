# ParentProfile Domain

ParentProfile 도메인은 부모님 상세 프로필 작성, 수정, 조회와 추천용 여행 MBTI 현재값 저장을 담당합니다.

---

## 책임

- 부모 사용자가 본인의 상세 프로필을 단계별로 저장합니다.
- 부모 사용자가 본인의 상세 프로필을 완료 처리하면 추천용 여행 MBTI를 계산해 저장합니다.
- 부모 본인만 프로필을 작성/수정할 수 있습니다.
- 같은 가족으로 연결된 자녀는 부모님 프로필을 조회만 할 수 있습니다.
- 여행 테마는 현재 구현에서 최대 3개까지 enum code로 저장합니다.

---

## 패키지 구조

```text
parentprofile
├── controller
├── dto
├── entity
├── repository
└── service
```

---

## 관련 테이블

- `parent_profiles`
- `parent_profile_themes`
- `users`
- `family_members`

---

## 관련 API

- `GET /api/v1/parent-profiles/me`
- `PUT /api/v1/parent-profiles/me`
- `GET /api/v1/parent-profiles/{parentUserId}`

---

## 구현 결정

- `PUT /api/v1/parent-profiles/me`는 프로필 row가 없으면 생성하고 있으면 수정합니다.
- 부분 저장을 위해 요청 필드는 대부분 nullable로 두고, 전달한 필드만 반영합니다.
- 저장된 프로필이 없을 때 조회 API는 `profileExists=false`인 빈 `draft` 응답을 반환합니다.
- 상세 MBTI 이력 테이블은 아직 구현하지 않고, 현재 추천에 사용할 결과 1개만 `parent_profiles.personality_type`에 저장합니다.
