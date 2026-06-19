# profanity-filter-api sample FE

이 폴더는 실제 프론트엔드 후보를 판단하기 위한 작은 샘플 앱입니다.
현재 기본 화면은 공개 영역의 랜딩 페이지와 문서/예제 섹션에 집중합니다.
초기 콘솔 데모는 `src/demo/ConsoleDemo.tsx`로 분리해 보관합니다.

## 이 샘플로 판단할 수 있는 것

- Vite 프로젝트가 어떤 파일 구조를 갖는지
- React 컴포넌트가 랜딩 섹션을 어떻게 나누는지
- 공개 시작 가이드와 문서/예제 섹션을 어떻게 배치하는지
- k3s에 정적 웹 앱으로 배포할 때 어떤 Dockerfile/Nginx 설정이 필요한지
- AI가 이후 화면을 확장할 때 어느 파일을 고치면 되는지

## 실행 방법

```bash
cd ui/sample-app
npm install
npm run dev
```

운영 빌드:

```bash
npm run build
npm run preview
```

Vite의 기본 흐름은 `npm run dev`로 개발 서버를 띄우고, `npm run build`로 `dist/` 정적 파일을 만드는 방식입니다.

## 폴더 구조

```text
ui/sample-app
├── package.json          # FE 의존성과 실행 스크립트
├── index.html            # React 앱이 붙는 HTML entry
├── vite.config.ts        # Vite 설정
├── Dockerfile            # k3s 배포용 정적 웹 컨테이너
├── nginx.conf            # SPA fallback 포함 Nginx 설정
└── src/
    ├── main.tsx          # React 시작점
    ├── App.tsx           # 공개 랜딩 페이지
    ├── demo/             # 분리 보관한 이전 콘솔 데모
    └── styles.css        # 데모 스타일
```

## 각 도구가 맡는 일

## 색상 기준

샘플 앱의 기준 팔레트는 아래 3개 CSS 변수입니다.

```css
--ivory: #f6f1df;
--pine: #173b2f;
--sage: #a9b99a;
```

- `--ivory`: 페이지 배경, 카드/버튼 내부 배경
- `--pine`: 본문 텍스트, 테두리, primary 버튼 배경
- `--sage`: 패널 헤더 배지, 상태 영역 배경
- hover/focus: pine 배경과 ivory 글자를 반전해 선택 상태 표시

### Vite

개발 서버와 빌드 도구입니다.
Spring Boot에서 Gradle이 빌드 진입점인 것처럼, FE에서는 Vite가 개발/빌드 진입점입니다.

이 샘플에서는:

- `npm run dev`가 Vite 개발 서버를 실행합니다.
- `npm run build`가 배포용 `dist/`를 생성합니다.
- `Dockerfile`은 생성된 `dist/`를 Nginx에 올립니다.

### React

화면을 컴포넌트로 나누는 UI 라이브러리입니다.
이 샘플의 `App.tsx` 안에는 다음 컴포넌트가 있습니다.

- `Header`: 상단 제품 정보
- `ApiKeyPanel`: API Key 발급 흐름
- `FilterConsole`: 필터 API 테스트 콘솔
- `InquiryPanel`: 문의/요청 접수 흐름
- `StackPanel`: 선택한 기술 스택 설명

### TypeScript

JavaScript에 타입을 추가한 언어입니다.
이 샘플에서는 `FilterMode`, `FilterResponse`, `ApiIssueRequest` 같은 타입을 만들어 API 계약을 코드에서 볼 수 있게 했습니다.

### shadcn/ui

이 샘플에는 shadcn CLI를 아직 적용하지 않았습니다.
대신 shadcn/ui를 적용하면 어떤 식으로 버튼, 입력, 카드 컴포넌트를 분리할지 판단할 수 있도록 화면 구조를 컴포넌트 단위로 잡았습니다.

실제 프로젝트에서는 다음 단계에서 shadcn/ui를 붙이는 것이 적절합니다.

```bash
npx shadcn@latest init
npx shadcn@latest add button input textarea select card tabs badge
```

### TanStack Query

서버 API 호출 상태를 관리하는 도구입니다.
이 샘플은 아직 실제 백엔드 호출 대신 `fakeApi`를 사용합니다.
실제 구현 단계에서는 `fakeApi`를 `fetch('/api/v1/...')` 또는 API client 함수로 바꾸고 TanStack Query를 붙이면 됩니다.

### React Hook Form + Zod

폼 입력값과 검증을 관리하는 조합입니다.
샘플에서는 의존성을 과하게 늘리지 않기 위해 아직 적용하지 않았습니다.
실제 구현에서는 API Key 발급, 문의, 단어 제안 화면부터 적용하면 됩니다.

## k3s 배포 형태

```text
npm run build
  -> dist/
  -> Docker image
  -> Deployment
  -> Service
  -> HTTPRoute
```

이 샘플의 `Dockerfile`은 Vite 앱을 빌드한 뒤 `nginx:alpine`으로 정적 파일을 서빙합니다.

## 백엔드와 붙일 때 바꿀 곳

현재는 `src/App.tsx`의 `fakeApi`가 가짜 응답을 만듭니다.
실제 API와 붙일 때는 아래 흐름으로 바꾸면 됩니다.

- `issueApiKey`: `POST /api/v1/clients/register`
- `filterText`: `POST /api/v1/filter`
- `sendInquiry`: 문의 API가 없으므로 새 API를 만들거나 `mailto`/외부 폼으로 대체

## 이 샘플의 의도

이 샘플은 최종 디자인이 아닙니다.
“Vite + React로 만들면 이런 개발 단위가 생기고, 이런 파일을 고치며 확장한다”를 판단하기 위한 출발점입니다.
