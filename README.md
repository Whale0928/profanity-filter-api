<img width="1003" alt="image" src="https://github.com/Whale0928/profanity-filter-api/assets/75371249/924c007c-1b76-4c90-8b8b-ba361171e6f2">

# 한국어 비속어 필터 API 서비스

> API 인증 키 발급 후 사용 가능합니다. 문서 링크를 참조해 주세요
>
>  [OpenAPI JSON](https://api.kr-filter.com/openapi.json)
>
>  [Overview](https://api.kr-filter.com/overview.md)
>
>  [테스트 페이지](https://api.kr-filter.com/)
>
>  ~~레거시 API 주소: https://api.profanity.kr-filter.com~~ (지원 종료 예정)
>
> 헬스 체크
> - https://api.kr-filter.com/api/v1/ping
> - https://api.kr-filter.com/api/v1/health

- 기존 `x-api-key` 신규 발급은 중단 예정이며, 이미 발급된 키는 호환성 유지를 위해 유지합니다.
- Google/GitHub SSO 로그인용 JWT access token과 rotating refresh token을 지원합니다.
- OAuth2 Client Credentials 기반 외부 API Bearer 인증은 아직 미구현이며, 향후 대시보드의 API 클라이언트 발급 기능과 함께 제공할 예정입니다.

## Overview

이 서비스는 한국어 비속어를 모두 검출하고 필터링할 수 있는 무료 API입니다.

### 주요 특징

- **경량 필터링 엔진**: 정규식과 비속어 데이터베이스를 활용한 효율적인 필터링
- **고성능 검사**: `아호코라식 알고리즘`을 사용하여 빠르고, 정확한 비속어 검출
- **다양한 필터링 모드**: 빠른 검사(QUICK), 일반 검사(NORMAL), 대체 검사(FILTER) 지원
- **KISO 호환성**: [KISO 이용자 보호 시스템 API](https://www.safekiso.com/)와 유사한 스펙으로 구현

### 이용 대상

이 API는 주로 다음과 같은 사용자를 위해 설계되었습니다:

- 포트폴리오나 취미 프로젝트를 개발하는 학생 및 개발자
- 비영리 서비스를 운영하는 소규모 단체
- 비용은 최소화하면서 기본적인 비속어 필터링이 필요한 웹사이트/앱

예산 제약 없이 상업적 서비스에 활용하실 경우에는 [KISO 이용자 보호 시스템 API](https://www.safekiso.com/)를 권장합니다(월 약 7만원).

~~이 서비스는 개인 서버로 운영되므로 가용성은 보장되지 않지만,~~  2025-05 기준 OCP 환경으로 이관하였습니다.

기본적인 비속어 필터링 기능을 무료로 제공하는 데 의의가 있습니다.

## API Guide

### 인증 전환 방향

현재 외부 API 호출은 기존 `x-api-key` 헤더를 사용합니다. 사람의 대시보드 로그인은 Google/GitHub SSO 완료 후 발급되는 `LOGIN_JWT`와 rotating refresh token을 사용하며, 외부 API 인증과 분리되어 있습니다.

OAuth2 Client Credentials의 `/oauth2/token`, `client_id/client_secret`, 외부 API용 Bearer access token은 다음 단계의 범위입니다. 현재 외부 API에 제출된 Bearer token은 지원되지 않는 `OAUTH2_ACCESS_TOKEN` 경계에서 fail-closed 처리하며, 기존 API Key 동작은 유지합니다. 상세 계약은 [Authentication](profanity-api/src/main/resources/openapi/authentication.md)을 참고하세요.

- [ADR 0005. SSO 기반 사용자 계정 모델 도입](docs/adr/0005%20SSO%20기반%20사용자%20계정%20모델%20도입.md)
- [ADR 0006. OAuth2 Client Credentials 기반 API 인증 전환](docs/adr/0006%20OAuth2%20Client%20Credentials%20기반%20API%20인증%20전환.md)

### 현재 API 호출 방식

- 요청 URL: `POST https://api.kr-filter.com/api/v1/filter`
- headers
	- `Content-Type: application/json` or `application/x-www-form-urlencoded`
	- `accept: application/json`
	- `x-api-key: {API_KEY}`
		- API_KEY는 제공되는 API_KEY를 사용해주세요.
- parameters:
	- *`text`: 검증할 문장 (예: "나쁜말")
	- *`mode`: `QUICK`,`NORMAL`,`FILTER` 중 하나 선택
		- `QUICK`: 빠른 검사에 적합합니다.
		- `NORMAL`: 일반적인 검사에 적합합니다.
		- `FILTER`: 일반적인 검사후 비속어를 `*`로 대체합니다.
	- `callbackUrl`: 비동기 처리시 결과를 받을 URL
	- \* 기호가 붙은 파라미터는 필수 입력값입니다.

### Response Code

- 요청에 대한 HTTP Status Code는 대부분 200으로 응답됩니다.
- 응답 객체에서는 요청에 따라 변동적인 `status.code`에 응답 코드가 포함되어 있습니다.
- `Status Code`는 KISO 이용자 보호 시스템 API 서비스의 응답 코드를 참조하여 작성되었습니다.

| Status Code | Description           | Description                                                  |
|-------------|-----------------------|--------------------------------------------------------------|
| 2000        | OK                    | 요청이 정상적으로 처리된 상태를 의미합니다.                                     |
| 2020        | Accepted              | 비동기 요청이 정상적으로 접수된 상태를 의미합니다.                                 |
| 2021        | Processing            | 요청 처리가 진행 중인 상태를 의미합니다.                                      |
| 4000        | Bad Request           | 요청이 비정상적인 경우 입니다. 파라미터 누락,타입 오류등이 있습니다, 상세 내용을 참고하세요.        |
| 4001        | Invalid Callback URL  | 콜백 URL 형식이 올바르지 않은 경우 발생합니다.                                 |
| 4002        | Invalid Tracking ID   | Tracking ID가 유효하지 않은 경우 발생합니다.                               |
| 4003        | Not Fount Tracking ID | Tracking ID를 찾을 수 없는 경우 발생합니다.                               |
| 4004        | Ambiguous Credentials | 다중 또는 중복 인증 정보를 제출한 경우 발생합니다.                               |
| 4010        | Unauthorized          | 요청을 인증할 API 키 값이 없는 경우 발생하는 오류 입니다.                          |
| 4011        | OAuth2 Login Failed   | Google/GitHub SSO 로그인에 실패한 경우 발생합니다.                         |
| 4012        | Login Code Invalid    | 로그인 교환 코드가 잘못됐거나 만료 또는 재사용된 경우 발생합니다.                       |
| 4013        | Login Token Invalid   | 로그인 access token 검증에 실패한 경우 발생합니다.                          |
| 4014        | Login Token Expired   | 로그인 access token이 만료된 경우 발생합니다.                              |
| 4015        | Refresh Token Invalid | refresh token 또는 session이 잘못됐거나 만료·폐기된 경우 발생합니다.              |
| 4016        | Refresh Token Reused  | 이미 소비된 refresh token이 다시 제출된 경우 발생합니다.                         |
| 4017        | OAuth2 Token Unsupported | 외부 API용 OAuth2 access token이 아직 지원되지 않는 경우 발생합니다.             |
| 4030        | Forbidden             | 서버에서 요청에 API 키값을 인식하였으나 해당 키가 적절한 권한을 가지지 않았다고 판정한 경우 발생합니다. |
| 4031        | Not Found Client      | API Key에 해당하는 클라이언트 정보를 찾을 수 없는 경우 발생합니다.                    |
| 4032        | Invalid API Key       | API Key가 유효하지 않은 경우 발생합니다.                                   |
| 4033        | User Inactive         | 로그인 사용자가 비활성 상태인 경우 발생합니다.                                  |
| 4290        | Too Many Requests     | 특정 클라이언트가 너무 많은 요청을 단위 시간 안에 보낸 경우에 이 응답이 리턴됩니다.             |
| 5000        | Internal Server Error | 서버 측의 문제로 요청에 대한 처리가 불가능한 경우 오류가 발생하였음을 알리기 위해 본 코드를 사용합니다.  |
| 5030        | Service Unavailable   | 서비스 점검 또는 일시 사용 불가 상태를 의미합니다.                                |

### Usage Guide

#### **응답 예**

```json
{
	"trackingId": "bee20667-aa5a-4d39-94f5-0f2dcbd51cac",
	"status": {
		"code": 2000,
		"message": "Ok",
		"description": "정상적으로 처리 되었습니다.",
		"DetailDescription": ""
	},
	"detected": [
		{
			"length": 1,
			"filteredWord": "나"
		},
		{
			"length": 2,
			"filteredWord": "나쁜"
		},
		{
			"length": 3,
			"filteredWord": "나쁜말"
		},
		{
			"length": 2,
			"filteredWord": "냐쁀"
		}
	],
	"filtered": "*** 이런 개 ** 짓을 왜 하냐?, **, *",
	"elapsed": "0.00007676 s / 0.07676 ms / 76.758 µs"
}
```

## Examples

- [cURL Guide](examples/curl.md)
- [Java Guide](examples/java.md)
- [JavaScript Guide](examples/javascript.md)

## 주의사항

### 서비스 이용 제한

- **사용 목적**: 이 서비스는 포트폴리오, 학습용 프로젝트, 비영리 서비스를 위해 제공됩니다. 상업적/영리 목적으로 활용하시려면 [KISO 이용자 보호 시스템 API 서비스](https://www.safekiso.com/)를 이용해 주세요.

- **가용성**: 개인 서버로 운영되는 무료 서비스이므로 100% 가용성을 보장하지 않습니다. 서버 장애, 하드웨어 문제, 네트워크 이슈 등으로 일시적 중단이 발생할 수 있습니다.

- **성능 제한**: 과도한 API 호출 시 서비스 품질 유지를 위해 요청 제한(rate limiting)이 적용될 수 있습니다.

### 기타 고려사항

- **API 변경**: API 스펙은 개선을 위해 변경될 수 있습니다. 변경 시에는 이 문서를 통해 사전 공지할 예정입니다.

- **개인정보**: API를 통해 전송되는 텍스트는 비속어 필터링 목적으로만 사용되며, 별도로 저장하지 않습니다. 단, 서비스 개선을 위한 기본적인 사용 통계는 수집될 수 있습니다.

- **책임 제한**: 이 API의 결과에 의존하여 발생한 문제(필터링 실패, 잘못된 검출 등)에 대해 개발자는 법적 책임을 지지 않습니다.

이 서비스는 최대한 KISO 이용자 보호 시스템 API 서비스와 유사한 형태로 발전해 나갈 예정이지만, 무료 서비스로서의 한계가 있음을 양해해 주시기 바랍니다.

#### 문의사항은 Issue로 등록하거나 이메일로 문의바랍니다.

[ Post. 비속어 검증 API 서비스 만들기 ](https://deadwhale.me/posts/profanity-filter-api/)

## 문의 및 연락처

문의사항은 Issue로 등록하거나 이메일로 문의바랍니다.


<table align="center">
  <tr>
    <td align="center">
      <a href="https://github.com/Whale0928">
        <img src="https://github.com/Whale0928.png" width="100px;" alt=""/>
      </a>
      <br />
      📧 <a href="mailto:rlagusrl928@gmail.com">rlagusrl928@gmail.com</a>
      <br />
    </td>
  </tr>
</table>
