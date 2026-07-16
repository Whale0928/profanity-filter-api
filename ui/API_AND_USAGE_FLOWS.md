# API와 사용 플로우

기준: 2026-07-17 운영 `https://api.kr-filter.com/openapi.json`과 `main` 브랜치의 인증 정책.

## 1. 제공 API

인증 경계는 다음 세 가지로 분리된다.

| 구분 | Credential | 사용 범위 |
| --- | --- | --- |
| Public | 없음 | SSO 시작·교환·갱신, 문서, 상태 확인 |
| Login JWT | `Authorization: Bearer {accessToken}` | 로그인 사용자와 API Key 관리 |
| API Key | `x-api-key: {apiKey}` | 필터, 단어 관리, 동기화 |

Login JWT를 API Key 대신 사용할 수 없으며, 현재 외부 API용 OAuth2 access token은 지원하지 않는다.

### SSO 진입

| Method | Path | 역할 |
| --- | --- | --- |
| GET | `/oauth2/authorization/github` | GitHub SSO 시작 |
| GET | `/oauth2/authorization/google` | Google SSO 시작 |
| GET | `/login/oauth2/code/{provider}` | 서버가 처리하는 OAuth callback |

### 로그인 인증

| Method | Path | 인증 | 역할 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/exchange` | Public | 일회용 로그인 코드를 access token과 refresh cookie로 교환 |
| GET | `/api/v1/auth/csrf` | Public | refresh 요청에 사용할 CSRF token 조회 |
| POST | `/api/v1/auth/refresh` | Refresh cookie + CSRF | access token 갱신과 refresh token rotation |
| GET | `/api/v1/auth/me` | Login JWT | 현재 로그인 사용자 조회 |

로그아웃 endpoint는 아직 없다.

### API Key 발급과 관리

| Method | Path | 인증 | 역할 |
| --- | --- | --- | --- |
| GET | `/api/v1/dashboard/keys` | Login JWT | 본인의 활성·만료 API Key 목록 조회 |
| POST | `/api/v1/dashboard/keys` | Login JWT | `name`, `issuerInfo`, `note`로 신규 API Key 발급 |
| POST | `/api/v1/dashboard/keys/{keyId}/reissue` | Login JWT | 기존 키 만료 후 대체 키 발급 |
| DELETE | `/api/v1/dashboard/keys/{keyId}` | Login JWT | API Key 만료 |

입력 이메일은 받지 않고 Login JWT 사용자의 primary email을 사용한다. API Key 원문은 발급·재발행 응답에서만 한 번 반환한다. 기존 `/api/v1/clients/**`는 삭제됐다.

### 비속어 필터

| Method | Path | 인증 | 역할 |
| --- | --- | --- | --- |
| POST | `/api/v1/filter` | API Key | `QUICK`, `NORMAL`, `FILTER` 모드의 문장 필터링 |
| POST | `/api/v1/filter/advanced` | API Key | `word` query 기반 단일 필터링 |

`/api/v1/filter`에 `callbackUrl`이 없으면 동기 결과를 반환하고, 값이 있으면 접수 결과를 반환한 뒤 같은 `trackingId` 기준으로 callback을 시도한다.

### 단어 관리

| Method | Path | 인증 | 역할 |
| --- | --- | --- | --- |
| POST | `/api/v1/word/request` | API Key | 단어 추가·제거·수정 요청 |
| POST | `/api/v1/word/accept/{requestId}` | API Key + WRITE 권한 | 단어 변경 요청 승인 |

### 운영

| Method | Path | 인증 | 역할 |
| --- | --- | --- | --- |
| GET | `/api/v1/sync` | API Key + `password` query | 비속어 데이터 수동 동기화 |
| GET | `/api/v1/health` | Public | 상태 확인 |
| GET | `/api/v1/ping` | Public | 연결 확인 |
| GET | `/openapi.json` | Public | 운영 OpenAPI 문서 |
| GET | `/overview.md` | Public | API 개요 문서 |
| GET | `/llms.txt`, `/llm.txt` | Public | LLM용 문서 |

## 2. 사용 플로우

### 사람 로그인

1. 사용자가 GitHub 또는 Google 로그인을 선택한다.
2. 브라우저를 `/oauth2/authorization/{provider}`로 이동한다.
3. 서버가 OAuth callback을 처리하고 frontend 로그인 경로의 URL fragment에 60초 수명의 일회용 `code`를 전달한다.
4. UI가 `POST /api/v1/auth/exchange`로 코드를 한 번 교환한다.
5. access token은 응답 body에서 받아 메모리에만 보관하고, refresh token은 `HttpOnly` cookie로 유지한다.
6. UI가 access token으로 `GET /api/v1/auth/me`를 호출해 사용자 정보를 확인한다.
7. access token 갱신이 필요하면 `GET /api/v1/auth/csrf` 후 반환된 header 이름과 token으로 `POST /api/v1/auth/refresh`를 호출한다.
8. 갱신 응답의 새 access token으로 메모리 상태를 교체한다.

### 기존 API Key 자동 연결

1. 기존 `clients` 데이터는 V4 migration에서 같은 ID의 `api_keys`로 복제되고 원문은 SHA-256 hash로 전환된다.
2. 사용자가 SSO 로그인하면 검증된 primary email을 기준으로 소유자가 없는 기존 키를 비동기로 조회한다.
3. 동일 이메일의 미이관 키에만 현재 `users.id`를 연결한다.
4. 이미 소유자가 있거나 이메일이 다른 키는 변경하지 않으며 이후 로그인은 no-op이다.

### API Key 신규 발급

1. 로그인 사용자가 이름, 발급자 정보와 선택 메모를 입력한다.
2. UI가 Login JWT로 `POST /api/v1/dashboard/keys`를 호출한다.
3. 서버는 요청 이메일 대신 SSO primary email로 키를 발급한다.
4. 반환된 API Key 원문을 완료 화면에서 한 번 노출하고 안전하게 복사하도록 한다.
5. 이후 외부 API 호출에는 `x-api-key` 헤더를 사용한다.

API Key 원문 복구 기능은 없다. 분실한 키는 재발행한다.

### 필터 사용

1. 사용자가 API Key, 검사 문장, 처리 모드를 입력한다.
2. UI가 `x-api-key` 헤더와 함께 `POST /api/v1/filter`를 호출한다.
3. `QUICK`은 첫 감지, `NORMAL`은 감지 목록, `FILTER`는 마스킹된 문장을 보여준다.
4. HTTP 상태와 별도로 응답 body의 `status.code`를 성공·실패 판단 기준으로 사용한다.
5. `trackingId`, 감지 목록, 필터링 결과와 오류 상세를 구분해 보여준다.
6. 비동기 모드에서는 `callbackUrl`을 전달하고 접수 상태와 최종 callback 결과를 별도 단계로 취급한다.

### API Key 관리

1. Login JWT로 `/api/v1/dashboard/keys`를 호출해 본인의 활성·만료 키를 확인한다.
2. 재발행은 기존 키를 즉시 만료하고 새 키 원문을 한 번 반환한다.
3. 만료는 soft-expire로 반복 요청해도 같은 만료 시각을 유지한다.
4. 재발행과 만료는 기존 credential에 영향을 주므로 UI에서 실행 전 확인 단계를 둔다.

### 단어 변경 요청

1. 사용자가 단어, 사유, 심각도와 요청 타입을 입력한다.
2. UI가 `POST /api/v1/word/request`로 요청한다.
3. WRITE 권한을 가진 클라이언트만 request ID를 사용해 `/accept/{requestId}`로 승인한다.
