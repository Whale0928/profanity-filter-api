# UI Engineering Policy

이 문서는 `ui/` 영역의 코드 리팩토링, 구현, 테스트 기준이다. 새 화면과 메뉴는 점진적으로 추가하되, 미래 기능을 미리 구현하지 않는다.

## Core Philosophy

- OOP: 모듈은 데이터 묶음이 아니라 책임과 계약을 가진 객체처럼 다룬다. 외부에는 사용법을 드러내고 내부 상태와 브라우저 effect는 숨긴다.
- YAGNI: 아직 쓰지 않는 화면, API client, 상태 저장소를 만들지 않는다. 단, route registry, 테스트, 작은 리팩토링처럼 미래 변경을 안전하게 만드는 구조는 허용한다.
- Tell, Don't Ask: 호출자가 route 종류를 직접 묻고 분기하지 않는다. route definition이 `matchPath()`와 `render()` 계약을 제공한다.
- Separation of Concerns: routing, browser history, clipboard, API fetch, form state, view markup을 같은 함수에 섞지 않는다.
- Evolutionary Design: 작은 테스트로 현재 동작을 고정하고, 필요한 만큼만 구조를 개선한다.

## Refactoring Rules

- 리팩토링은 사용자-visible 동작을 바꾸지 않는다.
- 먼저 기존 화면 캡처와 주요 동작을 확인한다.
- 변경 전 실패하는 테스트를 추가하고 Red, Green, Refactor 순서를 지킨다.
- 3개 이상 파일을 바꾸는 변경은 파일 책임 목록을 먼저 정리한다.
- 기존 사용자 변경은 되돌리지 않는다.

## Hooks Rules

- React 공식 Rules of Hooks를 따른다.
- hook 이름은 반드시 `use`로 시작한다.
- hook은 React 함수 컴포넌트 또는 다른 hook의 top level에서만 호출한다.
- 브라우저 effect는 hook으로 격리한다. 예: history, popstate, clipboard, timer.
- hook은 command 이름을 명확히 반환한다. 예: `navigate`, `copy`, `reset`.
- 실패 상태를 숨기지 않는다. clipboard, fetch, parsing 실패는 UI가 표현할 수 있는 상태로 만든다.

## Routing Rules

- 메뉴와 route는 `src/app/routes.tsx`의 `APP_ROUTES`가 source of truth다.
- 새 메뉴는 route object를 추가해 확장한다.
- route object는 `path`, `label`, `showInNavigation`, `matchPath`, `render`를 가진다.
- prefix route는 경계를 명시한다. `/docs`는 `/docs`와 `/docs/...`만 매칭하고 `/docs-admin`은 매칭하지 않는다.
- fallback route는 홈으로 둔다.

## Testing Rules

- Red, Green, Refactor를 지킨다.
- 문자열 계약 검사는 `scripts/check-*.mjs`에 둔다.
- 화면 동작은 Playwright 또는 브라우저 기반 smoke check로 확인한다.
- 검증 없이 완료를 주장하지 않는다.
- test skip, 빈 catch, 타입 회피, lint disable로 문제를 우회하지 않는다.

## API Rules

- API 호출은 컴포넌트에 직접 흩뿌리지 않는다.
- endpoint별 client 함수와 DTO 타입을 분리한다.
- HTTP 성공과 body `status.code` 성공은 별개로 해석한다.
- API key와 민감 값은 기본적으로 브라우저 저장소에 오래 보관하지 않는다.
