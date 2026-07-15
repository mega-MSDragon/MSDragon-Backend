# Policy Docs

서비스 기획, 추천 로직, 내부 계산 규칙처럼 코드만으로 의도를 파악하기 어려운 정책 문서를 관리합니다.

정책이 확정되거나 변경되면 구현 여부와 별개로 이 디렉터리에 먼저 기록합니다. 구현이 뒤따르는 경우 관련 API/DB/도메인 문서도 함께 갱신합니다.

## 문서 목록

| 문서 | 설명 |
|------|------|
| [parent-travel-mbti.md](./parent-travel-mbti.md) | 부모님 여행 MBTI 타입과 가중치 계산 정책 |
| [course-recommendation.md](./course-recommendation.md) | TourAPI 기반 여행 코스 추천 API 사용처와 가중치 정책 |
| [route-optimization.md](./route-optimization.md) | Tmap 기반 일자별 방문 순서와 경로 최적화 정책 |
| [trip-edit.md](./trip-edit.md) | 여행 기본정보 수정 권한과 코스 초기화 정책 |
| [trip-pledge.md](./trip-pledge.md) | 여행 10계명 후보, 확정본, 서명 완료 기준 정책 |
| [travel-mode.md](./travel-mode.md) | 여행 기간 기반 상태 전환과 여행 모드 진입 정책 |
