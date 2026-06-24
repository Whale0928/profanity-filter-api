# profanity-filter-api frontend

이 디렉터리는 `profanity-filter-api`의 프론트엔드 실험과 공개 문서 UI를 관리합니다.
현재 실제로 빌드하고 확인하는 앱은 `sample-app`입니다.

## 현재 구조

```text
ui/
├── README.md        # FE 영역 전체 안내
├── openapi.yaml     # 코드 기준으로 정리한 수동 OpenAPI 스펙
└── sample-app/      # Vite + React + TypeScript 샘플 앱
```

`sample-app`은 공개 랜딩 페이지와 `/docs` 문서 화면을 제공합니다.
이전 정적 목업은 제거했고, 앞으로 FE 판단과 수정은 `sample-app`을 기준으로 합니다.

## 현재 앱이 하는 일

- `/`: 서비스 소개용 공개 랜딩 페이지를 렌더링합니다.
- `/docs`: API 서버의 `https://api.kr-filter.com/openapi.json`을 읽어 API 문서를 렌더링합니다.
- `sample-app/public/overview.md`: 문서 첫 화면에서 보여줄 시작 가이드와 오류 모델 설명을 제공합니다.
- `sample-app/src/demo/ConsoleDemo.tsx`: API Key 발급과 필터 테스트 콘솔 후보 화면입니다. 아직 라우트에 연결하지 않았습니다.

## 실행

```bash
cd ui/sample-app
npm install
npm run dev
```

운영 빌드와 로컬 미리보기:

```bash
cd ui/sample-app
npm run build
npm run preview
```

문서 구조 검사:

```bash
cd ui/sample-app
npm run test:docs
```

## 기술 선택

현재 샘플 앱은 다음 조합을 사용합니다.

- Vite: 개발 서버와 정적 빌드 도구
- React: 랜딩 페이지와 문서 화면 컴포넌트 구성
- TypeScript: API 문서와 화면 상태 타입 안정성 확보
- Scalar API Reference: OpenAPI 문서 렌더링
- Nginx static container: k3s에서 정적 파일 서빙

향후 실제 콘솔 기능을 붙일 때 검토할 후보는 다음과 같습니다.

- TanStack Query: API 호출 상태, 캐시, 재시도 관리
- React Hook Form + Zod: 발급 폼, 문의 폼, 단어 제안 입력 검증
- shadcn/ui + lucide-react: 운영형 UI 컴포넌트와 아이콘 구성

## 앞으로 만들 화면 후보

현재 PR 범위는 공개 랜딩과 API 문서입니다.
콘솔이나 계정성 기능은 별도 작업으로 분리하는 것이 좋습니다.

- API Key 발급
- API Key 재발급과 폐기
- 비속어 필터 테스트 콘솔
- 단어 추가, 제거, 수정 요청
- 문의 접수

## 백엔드와 맞춰야 할 계약

- 문의 API를 만들지, `mailto`나 외부 폼으로 시작할지 결정해야 합니다.
- API Key를 브라우저에 저장할지, 사용자가 매번 입력하게 할지 결정해야 합니다.
- 관리자 단어 승인 API는 현재 백엔드 구현 상태를 확인한 뒤 화면 제공 여부를 정해야 합니다.
- 공개 문서의 OpenAPI JSON은 API 서버가 제공하는 `/openapi.json`을 source-of-truth로 둡니다.

## 배포 구상

```text
ui/sample-app
  -> npm run build
  -> dist/
  -> Docker image
  -> k3s Deployment
  -> Service
  -> HTTPRoute
```

처음에는 하나의 FE 앱 안에서 라우팅만 나눠도 충분합니다.
도메인을 분리할 경우 공개 페이지, 문서, 콘솔의 책임을 먼저 나눈 뒤 결정합니다.
