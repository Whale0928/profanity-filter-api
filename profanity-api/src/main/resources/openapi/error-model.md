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
