# profanity-filter-api FE 설명

이 폴더는 앞으로 분리 배포할 프론트엔드의 방향을 설명하기 위한 초안입니다.
실제로 판단 가능한 샘플 앱은 `../sample-app`에 있습니다.
`demo.html`은 초기 정적 목업으로 남겨둔 파일이며, 프레임워크 선택 판단용으로는 `../sample-app`을 보세요.

## 목표

- API Key 발급, 조회, 재발급, 폐기 흐름을 화면에서 처리한다.
- 비속어 필터 API를 직접 테스트할 수 있는 콘솔을 제공한다.
- 단어 추가/제거/수정 요청과 문의 접수를 받을 수 있게 한다.
- README와 RestDocs에 흩어진 정보를 사용자가 읽기 쉬운 제품 페이지로 정리한다.
- 백엔드와 별도로 빌드하고 k3s에 정적 웹 앱으로 배포한다.

## 추천 스택

```text
Vite + React + TypeScript
Tailwind CSS + shadcn/ui + lucide-react
TanStack Query
React Hook Form + Zod
Nginx static container on k3s
```

## Vite가 뭔가

Vite는 프론트엔드 개발 서버와 빌드 도구입니다.
개발 중에는 변경 사항을 빠르게 브라우저에 반영하고, 배포할 때는 정적 파일 묶음인 `dist/`를 만들어 줍니다.

이 프로젝트에 Vite를 추천한 이유:

- 별도 FE 배포에 잘 맞는다.
- 결과물이 정적 파일이라 k3s에서 Nginx로 단순하게 서빙할 수 있다.
- Next.js 같은 서버 런타임 없이도 충분하다.
- AI가 만든 React 화면을 빠르게 붙이고 확인하기 좋다.

## React가 뭔가

React는 화면을 작은 컴포넌트 단위로 나눠 만드는 UI 라이브러리입니다.
예를 들어 API Key 발급 폼, 요청 테스트 패널, 응답 JSON 뷰어, 문의 폼을 각각 독립 컴포넌트로 만들 수 있습니다.

이 프로젝트에 React를 추천한 이유:

- AI 기반 UI 생성 예시와 자료가 가장 많다.
- shadcn/ui, TanStack Query, React Hook Form 생태계가 성숙하다.
- 콘솔형 화면과 문서형 화면을 같이 만들기 쉽다.
- 나중에 관리자 화면까지 확장해도 선택지가 넓다.

## TypeScript가 뭔가

TypeScript는 JavaScript에 타입을 붙인 언어입니다.
백엔드 API 응답의 `status.code`, `trackingId`, `detected`, `filtered` 같은 구조를 타입으로 고정해 화면 버그를 줄입니다.

이 프로젝트에 TypeScript를 추천한 이유:

- API 계약이 명확해진다.
- AI가 생성한 코드의 실수를 컴파일 단계에서 더 빨리 잡는다.
- 요청/응답 DTO를 백엔드 문서와 맞춰 관리하기 좋다.

## Tailwind CSS가 뭔가

Tailwind CSS는 CSS 클래스를 조합해서 빠르게 화면을 만드는 스타일링 도구입니다.
별도 CSS 파일을 크게 키우지 않고도 간격, 색상, 레이아웃을 일관되게 맞출 수 있습니다.

이 프로젝트에 Tailwind를 추천한 이유:

- AI가 UI를 생성할 때 결과물이 안정적인 편이다.
- 빠르게 예쁜 화면을 만들기 쉽다.
- 디자인 시스템을 나중에 색상 토큰 중심으로 정리하기 좋다.

## shadcn/ui가 뭔가

shadcn/ui는 버튼, 입력창, 탭, 카드, 다이얼로그 같은 UI 컴포넌트를 프로젝트 코드로 가져와 쓰는 방식입니다.
일반적인 패키지처럼 숨겨진 컴포넌트를 import하는 게 아니라, 실제 컴포넌트 파일을 프로젝트 안에 둡니다.

이 프로젝트에 shadcn/ui를 추천한 이유:

- 기본 디자인 품질이 좋다.
- 컴포넌트 코드가 프로젝트 안에 있어 AI가 읽고 수정하기 쉽다.
- 발급 폼, 문의 폼, 콘솔, 테이블 같은 운영 UI를 빠르게 만들 수 있다.

## TanStack Query가 뭔가

TanStack Query는 서버 API 호출 상태를 관리하는 도구입니다.
로딩, 실패, 재시도, 캐시, 데이터 갱신 같은 반복 코드를 줄여 줍니다.

이 프로젝트에 TanStack Query를 추천한 이유:

- 클라이언트 정보 조회, API Key 재발급, 필터 요청 같은 서버 상태를 다루기 좋다.
- 같은 API를 여러 화면에서 호출해도 중복 요청과 캐시 관리를 맡길 수 있다.
- 백엔드 응답이 항상 HTTP 200이고 내부 `status.code`를 보는 구조라, 공통 응답 처리 레이어를 만들기 좋다.

## React Hook Form과 Zod가 뭔가

React Hook Form은 폼 상태 관리 도구이고, Zod는 입력값 검증 도구입니다.
예를 들어 이메일 형식, 필수 입력, callback URL 형식, mode 값 제한을 프론트에서 먼저 검증할 수 있습니다.

이 프로젝트에 추천한 이유:

- API Key 발급, 문의, 단어 제안처럼 폼이 많다.
- 백엔드 validation 전에 사용자에게 빠르게 오류를 보여줄 수 있다.
- 요청 DTO 타입과 검증 규칙을 함께 관리하기 좋다.

## 배포 구상

```text
ui app source
  -> npm run build
  -> dist/
  -> nginx:alpine image
  -> k3s Deployment
  -> Service
  -> HTTPRoute
```

권장 도메인 예시:

- `profanity.kr-filter.com`: 공개 소개 페이지
- `console.profanity.kr-filter.com`: API Key 발급과 콘솔
- `docs.profanity.kr-filter.com`: API 문서

처음에는 하나의 FE 앱 안에서 라우팅만 나눠도 충분합니다.

## 초기 화면 구성안

1. 홈
   - 서비스 소개
   - 무료 제공 범위
   - API 상태
   - 문서와 테스트 콘솔 진입

2. API Key 발급
   - 이름, 이메일, 사용 목적, 메모 입력
   - 발급 결과와 보관 안내 표시

3. 테스트 콘솔
   - text, mode, callbackUrl, API key 입력
   - JSON 요청과 form 요청 지원
   - 응답 JSON 표시

4. 내 클라이언트
   - API key 기반 내 정보 조회
   - 재발급, 폐기, 메모 수정

5. 단어 제안
   - 추가, 제거, 수정 요청
   - 사유와 심각도 입력

6. 문의
   - 이메일, 제목, 내용 입력
   - 백엔드 문의 API가 없으면 초기에는 mailto 또는 외부 폼으로 시작

## 먼저 맞춰야 할 백엔드 계약

- 문의 저장 API를 새로 만들지, 이메일 링크로 시작할지 결정한다.
- 요청문 저장 정책을 README와 실제 코드 기준으로 정리한다.
- rate limit이 실제로 없다면 문구를 수정하거나 구현 계획을 분리한다.
- 관리자 단어 승인 API는 현재 `acceptWord()`가 미구현이므로 별도 작업으로 둔다.
- API Key를 브라우저 localStorage에 저장할지, 사용자가 매번 입력하게 할지 결정한다.

## 실제 샘플 앱

`../sample-app`은 실제 Vite + React + TypeScript 프로젝트 구조를 가진 샘플입니다.
API 발급, 필터 테스트, 문의 화면, k3s 배포용 Dockerfile/Nginx 설정을 포함합니다.

실행:

```bash
cd ui/sample-app
npm install
npm run dev
```

정적 목업 파일:

- `demo.html`: 의존성 없이 열 수 있는 초기 목업
- `../sample-app`: 실제 FE 후보를 판단하기 위한 샘플 앱
