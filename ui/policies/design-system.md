# UI Design System Policy

이 문서는 `profanity-filter-api` UI의 시각 언어와 컴포넌트 규칙이다. 제품 화면은 조용한 콘솔형 도구로 읽혀야 한다.

## Design Philosophy

- 기능 설명보다 사용자가 판단할 입력, 상태, 결과, 오류를 먼저 배치한다.
- 장식은 구조를 설명할 때만 사용한다.
- 메뉴는 점진적으로 늘어난다. 첫 구조는 작게 유지하되 메뉴가 늘어도 위치, 활성 상태, density가 흔들리지 않아야 한다.
- 화면 문구는 짧고 직접적으로 쓴다.

## Design Tokens

기본 색상 token은 3개를 유지한다.

```css
--ivory: #f6f1df;
--pine: #173b2f;
--sage: #a9b99a;
```

- `--ivory`: 페이지 배경, 입력 배경, 보조 버튼 배경
- `--pine`: 본문 텍스트, 구조선, primary 버튼, 고정 네비게이션
- `--sage`: 상태 영역, 선택 배경, 보조 강조
- 새 색상은 상태 의미가 필요할 때만 추가한다. 예: danger, success, warning.
- token 이름은 색 자체보다 역할을 우선한다.

## Layout Rules

- 데스크톱은 필요한 경우 2열, 모바일은 1열을 기본으로 한다.
- 주요 콘텐츠 폭은 제한한다.
- 카드 안에 카드를 중첩하지 않는다.
- 반복 정보 단위만 카드로 표현한다.
- 고정 네비게이션이 있는 화면은 `scroll-margin-top` 또는 padding으로 가려짐을 방지한다.
- 긴 JSON, 오류, API key는 줄바꿈 또는 내부 스크롤로 처리한다.

## Landing Route Rules

- 첫 화면은 4개 블록으로 구성한다.
- 1번 블록은 hero 구조를 유지한다.
- 2번 블록은 프로젝트 정체성을 짧게 설명한다.
- 2번 블록은 한국어 문장 입력, 비속어 검출, 결과 모드, 서비스 연동 흐름을 보여준다.
- 3번 블록은 사용 시나리오를 영상형 흐름으로 보여준다.
- 3번 블록은 실제 API 응답처럼 보이는 데이터가 아니라 적용 장면을 설명하는 문장만 사용한다.
- 4번 블록은 푸터형 CTA로 두고 `신청하기`, `문서 보기` 버튼만 둔다.
- 랜딩에서 실제 요청 흐름을 실행하거나 실제 API 응답처럼 보이는 데이터를 노출하지 않는다.

## Typography Rules

- 기본은 system sans-serif다.
- letter-spacing은 0을 유지한다.
- viewport width에 직접 비례하는 과한 글자 크기 조정을 피한다.
- hero scale은 랜딩 첫 화면에만 쓴다.
- dashboard, form, docs sidebar는 작고 단단한 제목 체계를 쓴다.

## Component Rules

- 버튼은 명령이다. 상태 badge와 버튼을 시각적으로 구분한다.
- 입력은 label, value, validation, disabled 상태를 함께 고려한다.
- 메뉴 item은 `label`, `path`, `active` 규칙을 route metadata에서 받는다.
- 아이콘을 도입할 경우 lucide-react를 우선 검토한다.
- 화면 내 visible text로 기능 설명서를 길게 쓰지 않는다.

## State Rules

- 상태는 `idle`, `loading`, `success`, `failed`, `empty`처럼 명시한다.
- 색상만으로 상태를 전달하지 않는다.
- 오류는 원인과 다음 행동을 짧게 알려준다.
- 복사, 발급, 인증처럼 짧은 feedback은 1-2초 안에 자동 복귀할 수 있다.

## Menu Growth Rules

- 새 메뉴는 route registry에 추가한다.
- nav에 보이지 않는 route는 `showInNavigation: false`로 둔다.
- prefix route는 경계 조건을 테스트한다.
- 메뉴가 5개를 넘으면 desktop nav density와 mobile overflow 방식을 별도 결정한다.

## Verification Rules

- 디자인 변경은 실제 브라우저 화면으로 확인한다.
- 데스크톱과 모바일 viewport 중 최소 하나 이상을 캡처한다.
- 텍스트가 버튼, 카드, sidebar 밖으로 밀려나지 않는지 확인한다.
