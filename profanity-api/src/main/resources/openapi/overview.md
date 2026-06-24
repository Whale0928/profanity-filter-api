한국어와 영어 비속어를 검출하고, 필요하면 검출 단어를 `*`로 마스킹하는 API입니다.

### 시작하기

1. `POST /api/v1/clients/register`로 API Key를 발급합니다.
2. 보호된 API는 `x-api-key` 헤더를 포함해 호출합니다.
3. `POST /api/v1/filter`에 `text`와 `mode`를 전달합니다.

### 처리 모드

| Mode | 설명 |
| --- | --- |
| `QUICK` | 첫 번째 감지 단어를 빠르게 확인합니다. |
| `NORMAL` | 감지된 단어 목록을 반환합니다. |
| `FILTER` | 감지된 단어를 마스킹한 문장을 반환합니다. |

### 응답 기준

대부분의 실패는 HTTP 상태 코드가 아니라 응답 본문의 `status.code`로 구분합니다.
`callbackUrl`을 전달하면 요청은 접수 응답을 먼저 반환하고, 이후 같은 `trackingId` 기준으로 비동기 결과 전달을 시도합니다.
