<img width="1003" alt="image" src="https://github.com/Whale0928/profanity-filter-api/assets/75371249/924c007c-1b76-4c90-8b8b-ba361171e6f2">

[ 테스트 하러가기 ](https://api.profanity-filter.run/)

#### 개발 진행중인 프로젝트입니다 문의사항은 Issue로 등록하거나 이메일로 문의바랍니다.

[ Post. 비속어 검증 API 서비스 만들기 ](https://deadwhale.me/posts/profanity-filter-api/)

## History

- `7/1` 500+a 가량의 비속어 데이터베이스 추가
- `2024.06` ~ 현재 서비스 운영 시작

## overview

한국어와 영어 비속어를 모두 검증할 수 있는 서비스입니다.

운영은 제 개인 서버의 전기가 끊이지 않는 한 계속될 예정입니다.

만약 실제 운영 서비스에 사용할 목적이면서 비용에 대한 부담이 가능한 경우

[KISO 이용자 보호 시스템 API 서비스](https://www.safekiso.com/)를 사용해보세요.

월 7만원 가량의 비용으로 매우 높은 신뢰성을 가진 검증이 가능합니다.

다만 우리는 돈이 없지만 비속어는 막고 싶으니 제가 무료 서비스를 제공해보도록 하겠습니다.

### Note

- 정규식과 비속어 데이터베이스를 이용한 경량형 비속어 필터링 모델입니다.
- 비속어 검사의 경우 `아호코라식 알고리즘`을 사용하여 빠르게 검사합니다.
- 원색적인 욕설의 경우 대부분 필터링이 가능합니다.
- [KISO 이용자 보호 시스템 API 서비스](https://www.safekiso.com/)의 스펙과 유사하게 구현되어 있습니다.
- 영어 욕설의 경우 몇몇 단어들은 포함되어있지만 모든 단어가 포함되어있지는 않습니다. 추가적인 단어를 추가하고 싶으시면 이슈를 등록해주세요.

## API Guide

- 요청 URL: `POST https://api.profanity-filter.run/api/v1/filter/`
- headers
    - `Content-Type: application/json` or `application/x-www-form-urlencoded`
    - `accept: application/json`
    - `x-api-key: {API_KEY}`
        - API_KEY는 제공되는 API_KEY를 사용해주세요. (현재는 무료로 제공중입니다.)
- parameters:
    - *`text`: 검증할 문장 (예: "나쁜말")
    - *`mode`: `QUICK`,`NORMAL`,`FILTER` 중 하나 선택
        - `QUICK`: 빠른 검사에 적합합니다.
        - `NORMAL`: 일반적인 검사에 적합합니다.
        - `FILTER`: 일반적인 검사후 비속어를 `*`로 대체합니다.
    - `callbackUrl`: 비동기 처리시 결과를 받을 URL
    - \* 기호가 붙은 파라미터는 필수 입력값입니다.

### Response Code

- 요청에 대항 Http Status Code는 항상 200으로 응답됩니다.
- 응답 객체에서는 요청에 따라 변동적인 `status.code`에 응답 코드가 포함되어 있습니다.
- `Status Code`는 KISO 이용자 보호 시스템 API 서비스의 응답 코드를 참조하여 작성되었습니다.
- 몇몇 상태 코드의 경우 실제로 구현이 되지 않은 상태 코드가 있습니다. 이는 추후에 구현될 예정입니다.(안될수도)

| Status Code | Description           | Description                                                  |
|-------------|-----------------------|--------------------------------------------------------------|
| 2000        | OK                    | 요청이 정상적으로 처리된 상태를 의미합니다.                                     |
| 2020        | Accepted              | 비동기 요청이 정상적으로 접수된 상태를 의미합니다.                                 |
| 4000        | Bad Request           | 요청이 비정상적인 경우 입니다. 파라미터 누락,타입 오류등이 있습니다, 상세 내용을 참고하세요.        |
| 4010        | Unauthorized          | 요청을 인증할 API 키 값이 없는 경우 발생하는 오류 입니다.                          |
| 4030        | Forbidden             | 서버에서 요청에 API 키값을 인식하였으나 해당 키가 적절한 권한을 가지지 않았다고 판정한 경우 발생합니다. |
| 4290        | Too Many Requests     | 특정 클라이언트가 너무 많은 요청을 단위 시간 안에 보낸 경우에 이 응답이 리턴됩니다.             |
| 5000        | Internal Server Error | 서버 측의 문제로 요청에 대한 처리가 불가능한 경우 오류가 발생하였음을 알리기 위해 본 코드를 사용합니다.  |

### Usage Guide

#### bash

```bash
    curl -X POST "https://api.profanity-filter.run/api/v1/filter" \
    -H "accept: application/json" \
    -H "Content-Type: application/json" \
    -d '{"text": "나쁜말", "mode": "filter"}'
```

#### Java

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import java.util.Collections;

public class ProfanityFilterClient {

    private static final String API_URL = "https://api.profanity-filter.run/api/v1/filter/";
    private static final String API_KEY = "YOUR_API_KEY";

    public String filterProfanity(String text, String mode, String callbackUrl) {
        RestTemplate restTemplate = new RestTemplate();

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-api-key", API_KEY);

        // Set the request body
        String jsonInputString = String.format("{ \"text\": \"%s\", \"mode\": \"%s\", \"callbackUrl\": \"%s\" }", text, mode, callbackUrl);

        // Create the request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonInputString, headers);

        // Make the request
        ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, requestEntity, String.class);

        // Check the response
        if (response.getStatusCode().is2xxSuccessful()) {
            return "Request was successful. Response body: " + response.getBody();
        } else {
            return "Request failed. Response code: " + response.getStatusCode();
        }
    }
}
```

#### JavaScript

```JavaScript
const fetch = require('node-fetch');

const url = 'https://api.profanity-filter.run/api/v1/filter/basic';
const apiKey = 'YOUR_API_KEY';
const data = {
    text: '나쁜말',
    mode: 'FILTER',
    callbackUrl: 'http://example.com/callback'
};

fetch(url, {
    method: 'POST',
    headers: {
        'accept': 'application/json',
        'Content-Type': 'application/json',
        'x-api-key': apiKey
    },
    body: JSON.stringify(data)
})
    .then(response => response.json())
    .then(data => console.log('Request was successful:', data))
    .catch(error => console.error('Request failed:', error));
```

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

---

## 주의사항

- 이 서비스는 포트폴리오나 비영리 서비스를 할때 최소한의 금액이라도 절약하기 바라는 마음으로 제작되었습니다. 따라서 영리 목적으로 사용할
  경우에는 [KISO 이용자 보호 시스템 API 서비스](https://www.safekiso.com/)를 사용하세요.
- 이 서비스는 무료로 제공되는 서비스이므로, 제 서버컴퓨터가 불의의 사고 등으로 다운되는 경우가 있을 수 있습니다. 이 점 유의해주세요.
- API 스펙은 변경될 수 있습니다. 변경 시에는 문서를 통해 공지하겠습니다. 추구하고자 하는 모델은 [KISO 이용자 보호 시스템 API 서비스](https://www.safekiso.com/) 입니다.

> 추가적인 문의 사항은 rlagusrl928@gmail.com으로 연락 혹은 issue를 등록해주세요.
