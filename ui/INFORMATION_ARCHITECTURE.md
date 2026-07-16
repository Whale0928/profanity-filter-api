# UI Information Architecture

기준일: 2026-07-17

이 문서는 UI의 메뉴, route, 접근 권한과 초기 사용 흐름을 정의한다. 시각 디자인과 컴포넌트 구현은 이 구조를 변경하지 않는다.

근거 문서:

- [ADR 0005. SSO 기반 사용자 계정 모델 도입](../docs/adr/0005%20SSO%20기반%20사용자%20계정%20모델%20도입.md)
- [ADR 0006. 로그인 기반 API 자격 증명 발급과 OAuth2 인증 도입](../docs/adr/0006%20OAuth2%20Client%20Credentials%20기반%20API%20인증%20전환.md)
- [API와 사용 플로우](./API_AND_USAGE_FLOWS.md)

## 제품 원칙

- 사람 로그인과 외부 API 호출 인증을 분리한다.
- 사람 로그인은 Google 또는 GitHub SSO만 제공한다.
- API Key를 먼저 제공하고 OAuth2 Client Credentials는 후속 업데이트로 제공한다.
- 두 자격 증명은 Google 또는 GitHub SSO 로그인 후에만 발급한다.
- 비로그인 공개 API Key 발급은 제공하지 않는다.
- API Key 발급·관리는 실제 API를 연결하고 OAuth2 Client Credentials만 준비 중으로 표현한다.
- UI가 호출하는 제품 API는 로그인·문서·본인 API Key 관리 API로 제한한다.

## 메뉴 트리

```text
Public
├── 소개
├── API 문서
└── 로그인

Signed-in utility
└── 현재 사용자
    ├── API Key 관리
    └── 내 계정
```

소개는 제품 설명, 로그인 후 시작 안내, 자격 증명 비교를 포함하는 단일 개요 페이지다. 별도의 Signed-in 메뉴 행은 두지 않는다. 로그인 후에도 `소개`와 `API 문서`는 전역 링크로 접근하고, `로그인`은 현재 사용자 진입점으로 대체한다.

## Route 정의

| 영역 | 메뉴 | Route | 접근 | 초기 역할 |
| --- | --- | --- | --- | --- |
| Public | 소개 | `/` | 누구나 | 서비스 목적, 인증 전환 방향과 지원 범위를 설명 |
| Public | API 문서 | `/docs` | 누구나 | 운영 OpenAPI와 사용 문서를 읽기 전용으로 제공 |
| Public | 로그인 | `/login` | 비로그인 | Google·GitHub SSO 진입과 로그인 상태 표시 |
| Signed-in | 내 계정 | `/app/account` | Login JWT | 현재 사용자 기본 정보 읽기 전용 표시 |
| Signed-in | API Key 관리 | `/app/keys` | Login JWT | 키 목록·발급·재발행·만료 관리 |

기존 `/app`, `/app/credentials` 경로는 `/app/keys`로 이동하는 호환 경로로 유지한다.

### 접근 규칙

- 비로그인 사용자가 `/app`, `/app/account`, `/app/keys`에 접근하면 `/login`으로 이동한다.
- 로그인 사용자가 `/login`에 접근하면 `/app`으로 이동한다.
- 알 수 없는 route는 별도 기능으로 추정하지 않고 Not Found 상태를 표시한다.
- 권한이나 API가 없는 메뉴는 disabled 상태로 미리 노출하지 않는다. 단, 자격 증명 비교와 API 문서에서는 후속 OAuth2 방식을 `추후 제공` 상태로 명확히 구분해 미리 볼 수 있다.

## 메뉴 표현 규칙

### 테마와 타이포그래피

- 모든 Public 및 Signed-in 화면은 light mode와 dark mode를 동등하게 지원한다.
- 초기 진입은 운영체제의 `prefers-color-scheme`을 따르고, 사용자가 선택한 테마는 브라우저에 저장한다.
- 테마 전환은 전역 사용자 영역에서 언제든 접근할 수 있어야 한다.
- 테마 전환 컨트롤은 아이콘과 switch 상태만으로 표현하고 `다크`, `라이트` 문자를 반복 노출하지 않는다.
- light와 dark는 단순 색상 반전이 아니라 동일한 정보 위계, 대비와 상태 의미를 유지한다.
- 본문과 UI는 현대적인 한국어 sans-serif를 사용하고, 장식적인 serif나 손글씨 계열을 핵심 UI 글꼴로 사용하지 않는다.
- 색상만으로 active, warning, recommended 상태를 전달하지 않는다.

### 비로그인 상태

- 전역 메뉴에는 `소개`, `API 문서`, `로그인`만 표시한다.
- 비로그인 상태에서는 신규 발급, 신청하기, API Key 만들기 같은 CTA를 표시하지 않는다.
- 로그인 CTA는 Google 또는 GitHub SSO 선택으로 이어진다.

### 로그인 상태

- 전역 메뉴의 `로그인` 자리는 현재 사용자 진입점으로 바뀐다.
- 별도의 Signed-in 메뉴 행은 표시하지 않는다.
- `소개` 안에서 시작 안내와 자격 증명 비교를 연속된 섹션으로 제공한다.
- 우측 현재 사용자 메뉴에서 `API Key 관리`와 `내 계정`으로 진입한다.
- `Playground`, `단어 관리`, `운영` 메뉴는 초기 범위에 포함하지 않는다.

## 화면별 정보 구조

### 소개 및 개요

1. 한국어 비속어 필터 API라는 제품 정체성
2. 신규 사용자는 SSO 로그인 후 API Key 또는 OAuth2 Client Credentials를 발급한다는 안내
3. 로그인 상태에 맞춘 시작 안내
4. API Key와 OAuth2 Client Credentials 비교 및 생성 진입
5. 현재 문서 확인과 로그인으로 이어지는 행동
6. 비로그인 공개 API Key 발급 종료 안내

소개 화면은 API endpoint 목록이나 가짜 대시보드 데이터를 보여주지 않는다.

### API 문서

1. `/overview.md`의 `#` 제목을 그대로 사용하는 문서 앵커 메뉴
2. 문서 메뉴와 API 그룹 메뉴 사이의 구분선
3. `/openapi.json`의 tag 순서를 따르는 API 그룹 메뉴
4. 기본적으로 접힌 API 그룹과, 펼쳤을 때 summary를 표시하는 operation 앵커
5. Scalar 기반 OpenAPI reference
6. API Key와 OAuth2 Client Credentials 사용 방식의 구분 및 OAuth2의 `추후 제공` 상태

문서 화면은 읽기 전용이다. API 실행 버튼과 credential 입력 기능을 제공하지 않는다.
문서 내용은 운영 `/overview.md`와 `/openapi.json`을 애플리케이션 진입 시 선조회하고 같은 session에서 재사용한다. 두 요청 외의 제품 API를 호출하지 않는다.
문서 앵커를 선택하면 Markdown의 해당 `#` 제목으로 이동한다. API 그룹은 클릭으로 펼치거나 접고, operation summary를 선택하면 같은 `/docs` route의 hash만 변경하여 Scalar의 해당 operation으로 이동한다.

### 로그인

1. Google SSO
2. GitHub SSO
3. checking, anonymous, exchanging, authenticated, failed 상태
4. 로그인 이후 `/app`으로 이동

### 자격 증명

1. API Key: 빠르고 단순한 연동
2. OAuth2 Client Credentials: 운영·서버 간 연동 권장, 현재는 `추후 제공` 상태
3. 각 방식의 발급 순서와 환경 변수 기반 요청 예시
4. API Key CTA는 `/app/keys` 관리 화면으로 연결
5. 목록에서는 API Key, Client Secret, access token 원문을 표시하지 않음

소개 페이지의 하위 섹션에서 두 방식을 같은 높이와 같은 정보 순서로 비교한다. OAuth2 Client Credentials는 내용을 읽을 수 있게 유지하되 팻말 형태의 `추후 제공` 표시와 disabled 상태로 생성·복사 상호작용을 차단한다. Client Secret은 후속 구현에서도 별도의 발급 완료 단계에서 최초 1회만 제공한다.

### 내 계정

1. 표시 이름
2. primary email
3. avatar
4. 로그인 상태

초기 버전은 `GET /api/v1/auth/me`가 반환하는 정보만 표시한다. OAuth provider 연결·해제, 이름 수정, 탈퇴, 로그아웃은 해당 API가 마련되기 전까지 제공하지 않는다.

### API Key 관리

1. 활성·만료 API Key를 한 목록에서 상태 텍스트와 함께 표시
2. API Key 이름, 고정된 SSO 이메일, 발급자 정보와 선택 메모 입력
3. 발급·재발행 응답의 키 원문을 완료 dialog에서 최초 1회만 표시
4. 재발행은 현재 키가 즉시 만료됨을 확인한 뒤 실행
5. 만료는 되돌릴 수 없는 작업임을 확인한 뒤 실행
6. 목록에는 `keyHint`만 표시하며 원문 복구 기능은 제공하지 않음
7. loading, empty, error, processing 상태를 색상 외 텍스트와 `aria-live`로 전달

## 핵심 사용 흐름

### 로그인

```text
소개 또는 로그인
→ Google/GitHub 선택
→ 서버 OAuth callback
→ 일회용 code 교환
→ access token은 메모리에 보관
→ /api/v1/auth/me 확인
→ 시작
```

### 세션 복구

```text
앱 진입
→ CSRF token 조회
→ refresh cookie rotation
→ 새 access token을 메모리에 보관
→ /api/v1/auth/me 확인
→ 시작 또는 기존 route 복귀
```

### 문서 탐색

```text
소개 또는 Signed-in 전역 메뉴
→ API 문서
→ Overview / Authentication / OpenAPI reference 탐색
```

### 기존 API Key 연결

```text
SSO 로그인 완료
→ 검증된 primary email로 미이관 api_keys 조회
→ user_id가 없는 동일 이메일 키만 비동기 연결
→ 이후 로그인에서는 변경할 키가 없어 no-op
→ API Key 관리 목록에서 확인
```

### API Key 관리

```text
현재 사용자 → API Key 관리
→ 새 키 발급 또는 기존 키 선택
→ 발급·재발행 시 원문 최초 1회 복사
→ 목록에서는 keyHint와 상태만 확인
→ 필요 시 재발행 또는 만료
```

## UI API 허용 범위

현재 API 구현이 완료되기 전 UI 애플리케이션 코드는 다음 경로만 호출할 수 있다.

| Method | Path | 목적 |
| --- | --- | --- |
| GET | `/oauth2/authorization/google` | Google SSO 시작 |
| GET | `/oauth2/authorization/github` | GitHub SSO 시작 |
| POST | `/api/v1/auth/exchange` | 일회용 로그인 code 교환 |
| GET | `/api/v1/auth/csrf` | refresh용 CSRF token 조회 |
| POST | `/api/v1/auth/refresh` | 로그인 session 복구와 rotation |
| GET | `/api/v1/auth/me` | 현재 사용자 확인 |
| GET | `/api/v1/dashboard/keys` | 내 API Key 목록 조회 |
| POST | `/api/v1/dashboard/keys` | 새 API Key 발급 |
| POST | `/api/v1/dashboard/keys/{keyId}/reissue` | API Key 재발행 |
| DELETE | `/api/v1/dashboard/keys/{keyId}` | API Key 만료 |
| GET | `/openapi.json` | API 문서 조회 |
| GET | `/overview.md` | API 개요 조회 |

## 금지 및 보류 범위

### 삭제된 legacy API

- `/api/v1/clients/**` 전체
- 이메일 인증 코드 기반 API Key 복구

API Key 외부 호출 인증은 유지하지만 발급·관리에는 Login JWT만 허용한다.

### 현재 API 구현 전 보류

- API client 생성과 `client_id`, `client_secret` 최초 1회 노출
- `POST /oauth2/token`

OAuth2 Client Credentials만 준비 중 상태로 표시한다.

### API 구현 후 추가

- Client Credentials access token 기반 Playground
- 로그아웃, 계정 변경, OAuth provider 연결 관리

이 항목들은 현재 IA의 메뉴나 화면 수에 포함하지 않는다.
