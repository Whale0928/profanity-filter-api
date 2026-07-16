# Authentication

외부 API 호출은 API Key로 인증합니다.

```http
x-api-key: YOUR_SECRET_TOKEN
```

보호된 API는 발급받은 API Key를 `x-api-key` 헤더에 포함해야 합니다. API Key가 없거나 유효하지 않으면 응답 본문의 `status.code`로 실패 원인을 확인합니다.

### Credential 정책

- API Key와 `Authorization`을 동시에 보내거나 같은 인증 헤더를 중복 제출하면 HTTP `400`/code `4004`로 거부합니다.
- 로그인 JWT를 외부 API 인증으로 사용할 수 없습니다.
- 외부 API용 OAuth2 access token은 아직 지원하지 않으며 HTTP `401`/code `4017`로 종료합니다.

### 인증 실패

| Code | 조건 |
|---|---|
| `4004` | 다중 또는 중복 credential 제출 |
| `4010` | API Key 누락 |
| `4017` | 외부 API용 OAuth2 access token 미지원 |
| `4030` | 권한 부족 또는 차단된 클라이언트 |
| `4031` | 클라이언트 정보 없음 |
| `4032` | 유효하지 않은 API Key |
