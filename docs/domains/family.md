# Family Domain

Family 도메인은 가족 코드 발급과 부모-자녀 가족 매칭을 담당합니다.

---

## 책임

- 사용자별 고정 가족 코드를 발급/조회합니다.
- 가족 코드는 `MSH-0000` 형식으로 저장하며 입력 시 하이픈 유무를 모두 허용합니다.
- 상대방 가족 코드로 부모-자녀 관계를 생성합니다.
- 로그인 사용자의 가족과 구성원 프로필을 조회합니다. 마이페이지 프로필 카드가 쓰는 프리셋 아바타, 부모님 프로필 완료 여부, 여행 MBTI 유형을 함께 반환하므로 parentprofile 도메인의 `ParentProfileRepository`를 읽습니다.
- 자녀 1명, 부모 최대 2명 제약을 서비스에서 검증합니다.
- 회원가입 후 가족 연결이 완료되기 전에는 클라이언트가 홈 진입을 막고 `GET /api/v1/family`로 연결 상태를 확인합니다.
- 한 사용자가 여러 가족에 속하지 않도록 검증합니다.
- 매칭 이력을 `family_code_usages`에 기록합니다.
- 가족 관계 표시는 클라이언트 요청으로 받지 않고, 응답 시 부모 성별로 `엄마`/`아빠`를 계산합니다. 성별이 `undisclosed`이면 `null`입니다.

---

## 패키지 구조

```text
family
├── controller
├── dto
├── entity
├── repository
└── service
```

---

## 관련 테이블

- `families`
- `family_members`
- `family_codes`
- `family_code_usages`

---

## 관련 API

- `POST /api/v1/family/code`
- `POST /api/v1/family/matches`
- `GET /api/v1/family`

---

## 후속 범위

- 가족 구성원 프로필 수정 권한 정책
- 가족 초대 코드 만료/재발급 정책이 필요할 경우 코드 상태 전환 API 추가
