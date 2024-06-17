<img width="1003" alt="image" src="https://github.com/Whale0928/profanity-filter-api/assets/75371249/924c007c-1b76-4c90-8b8b-ba361171e6f2">

# profanity-filter-api

한국어와 영어 비속어를 모두 검증할 수 있는 서비스입니다.

운영은 제 개인 서버의 전기가 끊이지 않는 한 계속될 예정입니다.

만약 실제 운영 서비스에 사용할 목적이면서 비용에 대한 부담이 가능한 경우 [KISO 이용자 보호 시스템 API 서비스](https://www.safekiso.com/)를 사용해보세요.

월 7만원 가량의 비용으로 매우 높은 신뢰성을 가진 검증이 가능합니다.

다만 우리는 돈이 없지만 욕설은 막고 싶으니 제가 무료 서비스를 제공해보도록 하겠습니다.

----

## 소개

### Basic 모델

- 정규식과 비속어 데이터베이스를 이용한 경량형 비속어 필터링 모델입니다.
- 문장의 경우 비속어의 갯수가 아닌 포함 여부만 판단합니다.
- 비속어 검사의 경우 아호코라식 알고리즘을 사용하여 빠르게 검사합니다.
- 원색적인 욕설의 경우 대부분 필터링이 가능합니다.
- 부정적인 문장 , 비꼬는 문장의 경우까지 필터링이 필요한 경우 Advanced 모델을 사용해주세요.

### Advanced 모델

- 정규식 , 비속어 데이터베이스 , AI를 이용한 비속어 필터링 모델입니다.
- AI의 경우 인증되지 않은 사용자의 경우 1일 100회 사용 제한이 있습니다.

### 사용 예시

- 요청 경로 :
    - basic : `https://api.profanity-filter.run/api/v1/filter/basic?word = {{word}}`
    - advanced : `https://api.profanity-filter.run/api/v1/filter/advanced?word = {{word}}`

| 파라미터 | 설명      | 필수 | 타입     |
|------|---------|----|--------|
| word | 필터링할 문장 | O  | string |
