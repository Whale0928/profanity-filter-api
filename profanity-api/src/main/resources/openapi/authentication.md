# Authentication

인증이 필요한 API는 API Key를 HTTP 헤더에 포함해야 합니다.
클라이언트 등록과 이메일 인증, 헬스 체크, OpenAPI 문서 조회는 인증 없이 호출할 수 있습니다.

## API Key 헤더

```http
x-api-key: YOUR_SECRET_TOKEN
```

서버 구현은 대소문자를 구분하지 않는 HTTP 헤더 특성에 따라 `X-API-KEY` 값을 읽고, OpenAPI 문서에는 `x-api-key` 이름으로 노출합니다.

## 추가 예정: OAuth 2.0 Client Credentials

현재 인증 방식은 API Key 헤더 기반입니다.
향후에는 Toss API처럼 OAuth 2.0 Client Credentials Grant 방식의 액세스 토큰 발급을 추가할 예정입니다.
이 방식이 추가되면 클라이언트는 발급받은 `client_id`와 `client_secret`으로 액세스 토큰을 요청하고, 보호된 API 호출 시 `Authorization: Bearer {access_token}` 헤더를 사용할 수 있게 됩니다.

아직 이 흐름은 제공되지 않습니다.
현재 운영 가능한 인증 방식은 `x-api-key` 헤더입니다.

## 인증 흐름

1. 클라이언트 등록 API로 API Key를 발급합니다.
2. 보호된 API 호출 시 `x-api-key` 헤더에 API Key를 전달합니다.
3. 서버는 API Key로 클라이언트 메타데이터를 조회합니다.
4. 클라이언트 권한은 Spring Security 권한인 `ROLE_READ`, `ROLE_WRITE`, `ROLE_DELETE`, `ROLE_BLOCK`, `ROLE_DISCARD` 형태로 변환됩니다.
5. `@VerifiedClientOnly`가 적용된 API는 차단 또는 폐기된 클라이언트를 한 번 더 검사합니다.

## 인증이 필요하지 않은 경로

- `GET /`
- `GET /index.html`
- `POST /api/v1/clients/register`
- `GET /api/v1/clients/send-email`
- `PUT /api/v1/clients/send-email`
- `GET /api/v1/health`
- `GET /api/v1/ping`
- `GET /openapi.json`
- `GET /openapi/*.md`
- `GET /llms.txt`
- `GET /llm.txt`

## 인증 실패

API Key가 없으면 `4010 Unauthorized` 상태 코드가 반환됩니다.
API Key가 유효하지 않거나 클라이언트 정보를 찾을 수 없으면 `4032 Invalid_api_key` 또는 `4031 Not_found_client`가 반환됩니다.
차단되거나 폐기된 클라이언트는 `4030 Forbidden`으로 응답합니다.

```json
{
  "status": {
    "code": 4010,
    "message": "Unauthorized",
    "description": "인증 키가 누락 되었습니다.",
    "DetailDescription": ""
  },
  "data": null
}
```

## CORS

현재 서버는 모든 origin을 허용하고, `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS` 메서드를 허용합니다.
브라우저 클라이언트에서는 `x-api-key` 헤더를 포함해 요청할 수 있습니다.
