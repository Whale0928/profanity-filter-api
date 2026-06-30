# profanity-filter-api UI

`ui/`는 `profanity-filter-api`의 프론트엔드 작업 영역이다. 공개 랜딩, API 문서, 키 발급 화면, 이후 추가될 콘솔성 메뉴를 하나의 React 앱에서 점진적으로 확장한다.

## Structure

```text
ui/
├── README.md
├── openapi.yaml
├── policies/
│   ├── engineering.md
│   └── design-system.md
├── skills/
│   └── profanity-ui/SKILL.md
└── app/
    ├── package.json
    ├── src/
    └── scripts/
```

## Source Of Truth

- Engineering policy: `policies/engineering.md`
- Design system policy: `policies/design-system.md`
- Agent workflow: `skills/profanity-ui/SKILL.md`
- Runtime app: `app`
- Navigation registry: `app/src/app/routes.tsx`

## Current Routes

- `/`: 서비스 소개 랜딩
- `/register`: 키 발급 화면
- `/docs`: API 문서

## Commands

```bash
cd ui/app
npm ci
npm run dev
npm run typecheck
npm run test:policy
npm run test:routing
npm run test:hooks
npm run test:register
npm run test:docs
npm run build
```

## Working Rules

- 새 메뉴는 `APP_ROUTES`에 추가한다.
- route matching은 경계 조건을 테스트한다.
- 브라우저 effect는 custom hook으로 분리한다.
- 화면 변경 전후로 주요 route를 브라우저에서 확인한다.
- 디자인 변경은 `policies/design-system.md`를 먼저 따른다.
