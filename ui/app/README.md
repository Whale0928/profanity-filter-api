# profanity-filter-api App

이 앱은 `profanity-filter-api`의 프론트엔드 런타임이다. 정책과 작업 지침은 상위 `ui/` 디렉터리에 둔다.

## Routes

- `/`: 서비스 소개 랜딩
- `/register`: 키 발급
- `/docs`: API 문서

## Commands

```bash
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

## Runtime Boundaries

- `src/app/routes.tsx`: 메뉴와 route source of truth
- `src/hooks/useAppNavigation.ts`: history, popstate, scroll restoration
- `src/hooks/useClipboard.ts`: clipboard success/failure feedback
- `src/docs/*`: OpenAPI와 overview 문서 로딩
- `src/features/*`: route별 화면

## Deployment Shape

```text
npm run build
  -> dist/
  -> Docker image
  -> k3s Deployment
  -> Service
  -> HTTPRoute
```
