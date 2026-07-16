# Authentication

사람의 대시보드 로그인과 외부 API 호출 인증은 서로 다른 경계로 처리합니다.

| 인증 타입 | Credential | 허용 범위 |
|---|---|---|
| `API_KEY` | `x-api-key` | 기존 필터, 클라이언트, 단어 관리, 동기화 API |
| `LOGIN_JWT` | `Authorization: Bearer {access_token}` | `GET /api/v1/auth/me`, `/api/v1/dashboard/**` |
| `OAUTH2_ACCESS_TOKEN` | 미래의 Bearer access token | 아직 미지원, 외부 API에서 HTTP `401`/code `4017`로 종료 |

API Key와 `Authorization`을 동시에 보내거나 같은 인증 헤더를 중복 제출하면 HTTP `400`/code `4004`로 거부합니다. 로그인 JWT를 외부 API 인증으로 재사용할 수 없으며, 외부 API Bearer를 로그인 JWT로 추정하거나 fallback하지 않습니다.

### API Key

기존 외부 API는 다음 헤더를 사용합니다.

```http
x-api-key: YOUR_SECRET_TOKEN
```

기존 API Key의 성공, 권한, 오류 응답 계약은 유지됩니다.

### Google/GitHub SSO 로그인

1. 브라우저를 `GET /oauth2/authorization/google` 또는 `GET /oauth2/authorization/github`로 이동합니다.
2. OAuth2 callback 성공 시 서버는 frontend 로그인 경로의 URL fragment에 60초 수명의 일회용 `code`만 전달합니다.
3. frontend는 `POST /api/v1/auth/exchange`의 JSON body로 코드를 한 번 교환합니다.
4. 응답 body의 RS256 access token은 메모리에서만 사용하고, opaque refresh token은 `HttpOnly` cookie로만 전달됩니다.

```json
{
  "code": "ONE_TIME_EXCHANGE_CODE"
}
```

access token 기본 계약은 다음과 같습니다.

- 수명: 15분, clock skew: 30초
- 필수 검증: `alg=RS256`, `iss`, `aud`, `sub=users.id`, `iat`, `nbf`, `exp`, `jti`
- 용도 claim: `token_use=access`, `auth_type=LOGIN_JWT`
- authority: `AUTH_LOGIN_JWT`, `ROLE_USER`
- 매 요청마다 내부 사용자의 `ACTIVE` 상태를 다시 확인

### Refresh token rotation

- `GET /api/v1/auth/csrf`에서 CSRF token을 받은 뒤 `POST /api/v1/auth/refresh`를 호출합니다.
- refresh 요청은 `PF_LOGIN_REFRESH` HttpOnly cookie와 CSRF header/cookie가 모두 필요합니다.
- 정상 rotation은 기존 refresh token을 즉시 소비하고 새 access token과 새 refresh cookie를 발급합니다.
- refresh token 원문은 저장하지 않고 SHA-256 hash만 MySQL에 저장합니다.
- refresh token 수명은 14일이며, 세션의 절대 수명은 최초 로그인부터 30일입니다.
- 소비된 token이 rotation 후 5초 grace 안에 중복 제출되면 그 요청만 실패하고 winner token family는 유지합니다.
- grace 이후 소비된 token이 재사용되면 replay로 판단해 해당 refresh session family 전체를 폐기합니다.
- 사용자 비활성화, token/session 만료, 폐기, 잘못된 token은 refresh cookie를 만료시킵니다. grace 안의 중복 요청은 winner cookie를 보호하기 위해 cookie를 삭제하지 않습니다.

기본 refresh cookie 속성은 `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`입니다. 운영 프로필에서는 `Secure=true`가 아니면 애플리케이션이 시작되지 않습니다. 로그인 CORS는 명시적으로 허용된 frontend origin에만 credential을 허용합니다.

### 인증 제외 경로

- `GET /`, `GET /index.html`, 정적 리소스
- `GET /sso/**`
- `GET /oauth2/authorization/**`, `GET /login/oauth2/code/**`
- `POST /api/v1/auth/exchange`
- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/refresh` (refresh cookie와 CSRF로 자체 검증)
- `POST /api/v1/clients/register`
- `GET|PUT /api/v1/clients/send-email`
- `GET /api/v1/health`, `GET /api/v1/ping`
- `GET /openapi.json`, `GET /overview.md`, `GET /llms.txt`, `GET /llm.txt`

### 인증 실패

| Code | 조건 |
|---|---|
| `4004` | 다중 또는 중복 credential 제출 |
| `4010` | 기존 외부 API의 API Key 누락 |
| `4011` | SSO 로그인 실패 |
| `4012` | 로그인 교환 코드가 잘못됐거나 만료·소비됨 |
| `4013` | 로그인 access token이 잘못됨 |
| `4014` | 로그인 access token 만료 |
| `4015` | refresh token/session이 잘못됐거나 만료·폐기됨 |
| `4016` | 소비된 refresh token 재사용 |
| `4017` | 외부 API용 OAuth2 access token 미지원 |
| `4030` | 권한 부족, 차단, 폐기 상태 |
| `4031` | API Key에 해당하는 클라이언트 정보 없음 |
| `4032` | API Key 유효하지 않음 |
| `4033` | 로그인 사용자 비활성 상태 |

### 의도적으로 미구현된 범위

OAuth2 Client Credentials의 `/oauth2/token`, `client_id/client_secret`, 외부 API access token 발급·검증은 아직 제공하지 않습니다. 현재 외부 API의 Bearer credential은 `OAUTH2_ACCESS_TOKEN` 확장 경계에서 fail-closed 처리합니다.
