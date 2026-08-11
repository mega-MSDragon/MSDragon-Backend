# 부모님 여행 MBTI 정책

부모님 프로필 입력값을 기반으로 추천용 여행 성향 타입을 계산하는 정책입니다.
이 문서의 타입과 가중치는 코스 추천, 마이페이지 MBTI 상세, 여행 생성 입력 스냅샷에서 공통 기준으로 사용합니다.

## 입력 플로우

### 1단계: 이동 성향

| 필드 | 필수 | 선택 방식 | 값 | 화면 문구 |
|------|------|-----------|----|-----------|
| `walkingPace` | 예 | 단일 선택 | `slow` | 천천히 둘러보고 자주 쉬는 게 좋아요 |
| `walkingPace` | 예 | 단일 선택 | `normal` | 적당히 다니고 중간에 쉬는 게 좋아요 |
| `walkingPace` | 예 | 단일 선택 | `fast` | 여기저기 많이 다니는 걸 좋아해요 |
| `needsMobilityAssistance` | 예 | 단일 선택 | `true` | 네, 도움이 필요해요 |
| `needsMobilityAssistance` | 예 | 단일 선택 | `false` | 아니요, 괜찮습니다 |

`needsMobilityAssistance`는 선택 필수입니다. draft 저장 중에는 null일 수 있지만, 프로필 완료 시에는 반드시 `true` 또는 `false`여야 합니다.

### 2단계: 여행 취향

`travelThemes`는 최소 1개, 최대 3개까지 선택합니다.

| 값 | 화면 문구 | 예시 |
|----|-----------|------|
| `nature_scenery` | 자연·풍경 | 산, 바다, 공원 |
| `history_culture` | 역사·문화 | 유적지, 절, 문화재 |
| `shopping` | 쇼핑 | 백화점, 시장 |
| `activity` | 액티비티 | 레저, 물놀이, 체험 |
| `culture_life` | 문화생활 | 축제, 공연, 전시 |
| `landmark` | 랜드마크 | 건물, 공원, 명소 |
| `experience` | 체험 | 공예, 농장, 웰니스 |

### 3단계: 음식 취향

`foodPreference`는 단일 선택이며, 프로필 완료 시 필수입니다.

| 값 | 화면 문구 |
|----|-----------|
| `korean` | 한식 위주가 좋아요 |
| `familiar` | 한식·중식·일식 같은 익숙한 음식이 좋아요 |
| `adventurous` | 새로운 음식도 다 좋아요 |

## 여행 MBTI 타입

| 타입 코드 | 타입명 | 한 줄 문구 | 설명 |
|-----------|--------|------------|------|
| `urban_explorer` | 도시 취향 탐험가형 | 유명한 곳은 알차게 둘러봐야지. | 쇼핑, 문화생활, 랜드마크를 좋아하며 도시의 볼거리를 알차게 즐기는 타입이시네요. 적당히 움직이고 익숙한 음식을 편안하게 즐기시는 편이에요. |
| `culture_stroller` | 감성 문화 산책가형 | 좋은 곳에서는 천천히 쉬어가도 괜찮아. | 문화생활과 랜드마크를 좋아하며 여유 있게 도시를 둘러보는 타입이시네요. 중간중간 카페나 맛집에서 쉬어가는 일정을 선호하시는 편이에요. |
| `healing_traveler` | 유유자적 힐링러형 | 여행은 쉬러 가는 거지. | 자연풍경, 역사, 산책을 좋아하며 천천히 둘러보는 타입이시네요. 음식도 한식처럼 편안한 선택을 선호하시는 편이에요. |
| `heritage_walker` | 역사 산책가형 | 이야기가 있는 길을 걷는 게 좋아. | 역사적인 장소와 자연풍경을 좋아하며 적당히 걸으면서 여유를 챙기는 타입이시네요. 익숙한 음식 안에서 지역의 특색도 함께 즐기시는 편이에요. |
| `active_adventurer` | 액티비티 열정가형 | 가만히 있기엔 여행 시간이 아까워. | 액티비티와 체험, 이동이 많은 일정을 좋아하는 타입이시네요. 여러 장소를 둘러보고 새로운 음식에도 적극적으로 도전하시는 편이에요. |
| `local_challenger` | 로컬 도전가형 | 여행은 직접 해보고 먹어봐야지. | 체험형 여행과 새로운 음식, 현지 분위기를 좋아하는 타입이시네요. 유명 관광지만 보기보다 직접 경험하고 맛보는 데서 여행의 재미를 찾으시는 편이에요. |

프로필 완료 응답은 기존 `personalityType` 코드와 함께 위 내용을 `personalityResult`로 반환합니다. 결과 화면 제목에 사용할 부모 이름은 `parentDisplayName`으로 반환합니다.

## 가중치 계산

각 입력값이 타입별 점수에 가중치를 더합니다. 최종 점수가 가장 높은 타입을 부모님의 현재 여행 MBTI로 저장합니다.

### 이동 성향

| 입력 | 가중치 |
|------|--------|
| `walkingPace = slow` | `healing_traveler +4`, `culture_stroller +1`, `heritage_walker +1` |
| `walkingPace = normal` | `culture_stroller +2`, `heritage_walker +2`, `urban_explorer +1` |
| `walkingPace = fast` | `active_adventurer +5`, `local_challenger +3`, `urban_explorer +2` |
| `needsMobilityAssistance = true` | `healing_traveler +2`, `heritage_walker +1`, `culture_stroller +1` |
| `needsMobilityAssistance = false` | `active_adventurer +1`, `local_challenger +1`, `urban_explorer +1` |

### 여행 취향

여행 취향은 선택한 모든 카테고리의 가중치를 합산합니다.

| 입력 | 가중치 |
|------|--------|
| `nature_scenery` | `healing_traveler +4`, `heritage_walker +3` |
| `history_culture` | `heritage_walker +4`, `healing_traveler +1`, `culture_stroller +1` |
| `shopping` | `urban_explorer +4`, `culture_stroller +3` |
| `activity` | `active_adventurer +4`, `local_challenger +2` |
| `culture_life` | `culture_stroller +4`, `urban_explorer +2` |
| `landmark` | `urban_explorer +5`, `culture_stroller +3`, `heritage_walker +2` |
| `experience` | `local_challenger +5`, `active_adventurer +3` |

### 음식 취향

| 입력 | 가중치 |
|------|--------|
| `korean` | `healing_traveler +4`, `heritage_walker +3` |
| `familiar` | `urban_explorer +2`, `culture_stroller +2`, `heritage_walker +2` |
| `adventurous` | `local_challenger +5`, `active_adventurer +4` |

## 동점 처리

동점이 발생하면 아래 순서로 결정합니다.

1. 여행 취향에서 얻은 점수가 더 높은 타입
2. 이동 성향에서 얻은 점수가 더 높은 타입
3. 음식 취향에서 얻은 점수가 더 높은 타입
4. 고정 우선순위: `culture_stroller`, `urban_explorer`, `heritage_walker`, `local_challenger`, `healing_traveler`, `active_adventurer`

고정 우선순위는 완전히 같은 점수일 때만 사용하는 안정적인 tie-breaker입니다. 일반적인 결과 분포는 입력 가중치에서 결정되도록 합니다.

## 결과 분포 검증

걷기 속도 3종, 이동 도움 여부 2종, 음식 취향 3종, 여행 테마 1~3개 조합을 동일한 빈도로 가정한 전체 1,134개 유효 입력 조합의 결과입니다.

| 타입 코드 | 조합 수 | 비율 |
|-----------|---------|------|
| `urban_explorer` | 187 | 16.5% |
| `culture_stroller` | 190 | 16.8% |
| `healing_traveler` | 191 | 16.8% |
| `heritage_walker` | 189 | 16.7% |
| `active_adventurer` | 189 | 16.7% |
| `local_challenger` | 188 | 16.6% |

자동 테스트는 모든 타입이 나오고 각 결과 비율이 15% 이상 18% 이하인지 검증합니다. 실제 사용자 분포를 예측하는 수치는 아니며, 정책 자체가 특정 유형으로 치우치지 않는지 확인하는 기준입니다.

## 구현 규칙

- 프로필 완료 시 `walkingPace`, `needsMobilityAssistance`, `travelThemes`, `foodPreference`가 모두 있어야 합니다.
- draft 저장 중에는 일부 값이 비어 있을 수 있습니다.
- 부모가 프로필을 다시 작성하거나 MBTI를 새로 뽑으면 같은 정책으로 재계산하고 현재 결과를 덮어씁니다.
- 여행 생성 시점에는 현재 프로필 입력과 MBTI 결과를 해당 여행의 추천 스냅샷으로 보관합니다.
- 정책 버전이 바뀌면 이 문서를 먼저 갱신하고, 필요하면 기존 결과 재계산 여부를 별도 결정합니다.
