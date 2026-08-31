# Policy Docs

서비스 기획, 추천 로직, 내부 계산 규칙처럼 코드만으로 의도를 파악하기 어려운 정책 문서를 관리합니다.

정책이 확정되거나 변경되면 구현 여부와 별개로 이 디렉터리에 먼저 기록합니다. 구현이 뒤따르는 경우 관련 API/DB/도메인 문서도 함께 갱신합니다.

## 문서 목록

| 문서 | 설명 |
|------|------|
| [signup-onboarding.md](./signup-onboarding.md) | 소셜 로그인 이후 약관, 회원가입, 가족 연결 온보딩 정책 |
| [account-withdrawal.md](./account-withdrawal.md) | 회원 탈퇴 시 익명화, 인증 폐기, 가족 연결과 여행 기록 처리 정책 |
| [parent-travel-mbti.md](./parent-travel-mbti.md) | 부모님 여행 MBTI 타입과 가중치 계산 정책 |
| [course-recommendation.md](./course-recommendation.md) | TourAPI 기반 여행 코스 추천 API 사용처와 가중치 정책 |
| [route-optimization.md](./route-optimization.md) | Tmap 기반 일자별 방문 순서와 경로 최적화 정책 |
| [trip-edit.md](./trip-edit.md) | 여행 기본정보 수정 권한과 코스 유지 정책 |
| [trip-pledge.md](./trip-pledge.md) | 여행 10계명 후보, 확정본, 서명 완료 기준 정책 |
| [travel-mode.md](./travel-mode.md) | 여행 기간 기반 상태 전환과 여행 모드 진입 정책 |
| [trip-feedback.md](./trip-feedback.md) | 마지막 날 평가 요청과 부모별 여행 피드백 제출 정책 |
| [filial-report.md](./filial-report.md) | 부모 피드백 기반 효도 리포트 생성·집계 정책 |
| [nearby-support-facilities.md](./nearby-support-facilities.md) | 여행 모드 주변 공중화장실 적재·조회 정책 |
| [legal-documents.md](./legal-documents.md) | 개인정보처리방침·이용약관 정적 HTML 제공과 버전 관리 정책 |
| [mypage.md](./mypage.md) | 마이페이지 화면 구성, 프리셋 프로필 이미지, 알림·MBTI 재작성 정책 |
