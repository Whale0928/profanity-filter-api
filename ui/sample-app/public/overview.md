한국어 비속어 필터링 API는 문장 안의 부적절한 표현을 감지하고, 필요한 경우 검출된 단어를 마스킹해 반환합니다.
포트폴리오, 학습용 프로젝트, 비영리 서비스처럼 기본적인 댓글·게시글 검수가 필요한 환경에서 빠르게 붙여 쓸 수 있도록 설계했습니다.

## 시작하기

1. 클라이언트 등록 API로 API Key를 발급합니다.
2. 인증이 필요한 요청은 `x-api-key` 헤더에 발급받은 API Key를 포함합니다.
3. `/api/v1/filter`에 검사할 `text`와 처리 방식인 `mode`를 전달합니다.
4. 비동기 처리가 필요하면 `callbackUrl`을 함께 전달합니다.

## 주요 기능

- 문장 안의 한국어·영어 비속어를 검출합니다.
- 검출 결과를 목록으로 받거나, 검출된 단어를 `*`로 마스킹한 문장을 받을 수 있습니다.
- `callbackUrl`을 사용해 긴 처리 흐름을 비동기 방식으로 연결할 수 있습니다.
- 응답은 KISO 이용자 보호 시스템과 유사한 `status.code` 중심의 형식을 따릅니다.

## 처리 모드

- `QUICK`: 원색적인 표현을 빠르게 확인합니다.
- `NORMAL`: 등록된 비속어 데이터를 기준으로 감지된 단어 목록을 반환합니다.
- `FILTER`: 감지된 단어를 마스킹한 문장을 반환합니다.

## 비동기 처리

`/api/v1/filter` 요청에 `callbackUrl`을 포함하면 서버는 요청을 접수한 뒤 같은 `trackingId`로 비동기 결과 전달을 시도합니다.
즉시 결과가 필요한 경우에는 `callbackUrl` 없이 요청하면 됩니다.

## 응답 읽기

HTTP 상태 코드는 기본적으로 `200`으로 응답하고, 실제 처리 결과는 응답 본문의 `status.code`와 `status.message`에서 확인합니다.
각 요청의 추적에는 `trackingId`를 사용할 수 있습니다.


# Error Model

이 API는 HTTP 상태 코드보다 응답 본문의 `status` 객체를 기준으로 처리 결과를 전달합니다.
대부분의 실패 응답은 HTTP `200`으로 반환되고, 인증 필터에서 차단된 요청은 HTTP `401`이 함께 설정될 수 있습니다.

## 공통 오류 응답

필터 API를 제외한 대부분의 엔드포인트는 `ApiResponse<T>` 형식을 사용합니다.
오류 응답에서는 `data`가 `null`이며, 상세 원인은 `status.DetailDescription`에서 확인합니다.

```json
{
  "status": {
    "code": 4000,
    "message": "Bad_request",
    "description": "처리에 실패하였습니다. 요청이 잘못 되었거나 필수 파라미터가 누락된 경우 발생 합니다. Description에서 보다 상세한 오류 메세지를 확인할 수 있습니다.",
    "DetailDescription": "text: 필터링 대상 문자열은 필수입니다."
  },
  "data": null
}
```

## 필터 API 오류 응답

`/api/v1/filter`와 `/api/v1/filter/advanced`는 `FilterApiResponse` 형식을 사용합니다.
실패하더라도 요청 추적을 위해 `trackingId`가 포함될 수 있고, `detected`는 빈 배열, `filtered`는 빈 문자열로 반환됩니다.

```json
{
  "trackingId": "018f4fd8-9f6f-7d1a-9b80-3f1f8dd7c002",
  "status": {
    "code": 4001,
    "message": "Invalid_callback_url",
    "description": "콜백 URL 형식이 올바르지 않습니다. 콜백 URL을 확인해 주세요.",
    "DetailDescription": ""
  },
  "detected": [],
  "filtered": "",
  "elapsed": "0.00000000 s / 0.00000 ms / 0.000 µs"
}
```

## 주요 상태 코드

| Code | Message | 의미 |
| --- | --- | --- |
| `2000` | `Ok` | 요청이 정상 처리되었습니다. |
| `2020` | `Accepted` | 비동기 요청이 접수되었습니다. 결과는 callback URL로 전달됩니다. |
| `4000` | `Bad_request` | 필수 파라미터 누락, 유효성 검증 실패, JSON 파싱 실패 등 요청 형식 오류입니다. |
| `4001` | `Invalid_callback_url` | callback URL 형식이 올바르지 않습니다. |
| `4010` | `Unauthorized` | API Key가 누락되었습니다. |
| `4030` | `Forbidden` | 클라이언트 권한이 부적절하거나 차단·폐기된 클라이언트입니다. |
| `4031` | `Not_found_client` | API Key에 매칭되는 클라이언트 정보를 찾을 수 없습니다. |
| `4032` | `Invalid_api_key` | API Key가 유효하지 않습니다. |
| `4290` | `Too_many_requests` | 요청 횟수 제한을 초과했습니다. |
| `5000` | `Internal_server_error` | 서버 내부 오류입니다. |

## 상세 오류 메시지

`status.DetailDescription`은 요청별 상세 원인을 담습니다.
Bean Validation 오류는 필드별 메시지를 이어 붙여 반환하고, JSON 파싱 오류나 타입 불일치 오류는 요청 데이터 형식 문제를 설명합니다.


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
- `GET /overview.md`
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
