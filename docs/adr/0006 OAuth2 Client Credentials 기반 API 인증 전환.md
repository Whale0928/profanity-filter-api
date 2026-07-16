# 6. 로그인 기반 API 자격 증명 발급과 OAuth2 인증 도입

## Status
채택 (2026.07.17)

## Context
현재 외부 API 호출 인증은 `x-api-key` 헤더 중심이다. 이 방식은 단순하지만 API Key 원문이 매 요청마다 전달되고, 표준 OAuth2 기반 OpenAPI 문서화나 외부 개발자 경험 측면에서 한계가 있다.

앞으로는 SSO 기반 대시보드에서 인증된 사용자가 API 호출용 자격증명을 발급해야 한다. API Key는 빠르고 단순한 연동에 유용하고, OAuth2 Client Credentials는 짧은 수명의 Bearer token과 표준 인증 흐름이 필요한 운영 환경에 적합하다.

따라서 인증 경계는 다음처럼 분리되어야 한다.

- 대시보드 인증: Google/GitHub SSO 세션
- 간편 API 인증: 로그인 후 발급한 `x-api-key`
- 표준 API 인증: OAuth2 Client Credentials 방식의 Bearer access token
- 기존 API 인증: 이미 발급된 `x-api-key`

## Decision
API Key와 OAuth2 Client Credentials를 모두 제공하되, 두 자격 증명은 Google 또는 GitHub SSO 로그인 후 대시보드에서만 발급한다.

- 비로그인 공개 `POST /api/v1/clients/register`를 통한 API Key 발급은 종료한다.
- SSO 로그인 사용자는 대시보드에서 API Key 또는 OAuth2 Client Credentials를 선택해 발급한다.
- API Key는 간단한 연동을 위한 정식 자격 증명으로 계속 제공하며 `x-api-key` 헤더로 호출한다.
- 기존 API Key도 동일한 인증 경로에서 계속 검증한다.
- API 클라이언트 생성 시 `client_id`와 `client_secret`을 발급한다.
- `client_secret`은 최초 1회만 노출하고, 서버에는 원문이 아닌 hash를 저장한다.
- 토큰 발급 엔드포인트는 `POST /oauth2/token`으로 둔다.
- 1차 구현 범위는 `grant_type=client_credentials`만 허용한다.
- `/oauth2/token`은 `client_id`와 `client_secret`을 검증해 짧은 수명의 access token을 발급한다.
- 신규 API 호출은 `Authorization: Bearer {access_token}`을 사용한다.
- 외부 API 엔드포인트는 Bearer token과 `x-api-key`를 모두 허용한다.
- 대시보드 엔드포인트와 자격 증명 발급 API는 SSO 로그인 token만 허용하고, `x-api-key`로 접근할 수 없게 한다.

## Consequences
- 사용자는 연동 복잡도와 운영 요구에 따라 API Key와 Client Credentials 중 하나를 선택할 수 있다.
- OAuth2 인증은 표준 형식을 따르므로 OpenAPI 문서, SDK, 외부 개발자 경험을 개선할 수 있다.
- `client_id/client_secret`은 토큰 발급에만 사용하고, 실제 API 호출에는 짧은 수명의 access token을 사용한다.
- access token 검증 후 SecurityContext에는 API 호출 주체를 나타내는 principal을 세팅한다.
- `x-api-key`와 Bearer token은 인증 수단은 다르지만, 내부 권한 판단에는 유사한 principal 모델을 제공해야 한다.
- rate limit은 인증 방식이 아니라 내부 식별자 기준으로 적용한다. OAuth2 방식은 API client id, API Key 방식은 key id 또는 API client id를 기준으로 한다.
- 두 인증 방식을 함께 제공하므로 SecurityFilterChain, 인증 실패 응답, rate limit 헤더, 관측 로그에서 인증 타입을 명확히 구분해야 한다.
- API Key도 로그인 사용자와 소유 관계를 가져야 하며, 발급·재발급·폐기 작업은 대시보드 인증 경계 안에서 수행한다.

## Alternatives
- API Key만 제공: 구현은 가장 단순하지만 표준 인증과 짧은 수명 token이 필요한 운영 환경을 지원하지 못해 제외한다.
- OAuth2 Client Credentials만 제공: 인증 모델은 단순해지지만 간단한 연동의 진입 장벽이 커져 제외한다.
- 기존 `x-api-key`를 즉시 폐기: 보안 모델은 단순해지지만 기존 사용자의 API 호출을 깨뜨리므로 제외한다.
- OAuth2 Authorization Code를 API 호출 인증에 사용: 브라우저 사용자의 위임 인증에는 적합하지만 서버 간 API 호출용으로는 과하므로 제외한다.
- refresh token 제공: Client Credentials 1차 범위에서는 필요하지 않다. 만료 시 `client_id/client_secret`으로 access token을 다시 발급하면 된다.
- OAuth2 Authorization Server 전체 도입: 장기적으로 검토할 수 있으나, 현재 요구는 client credentials 전용 토큰 발급과 Bearer 검증이므로 1차 범위에서는 제외한다.
