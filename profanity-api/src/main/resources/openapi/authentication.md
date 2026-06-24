# Authentication

### API Key

보호된 API는 `x-api-key` 헤더가 필요합니다.

```http
x-api-key: YOUR_SECRET_TOKEN
```

### 인증 제외 경로

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

### 인증 실패

| Code | 조건 |
| --- | --- |
| `4010` | API Key 누락 |
| `4030` | 권한 부족, 차단, 폐기 상태 |
| `4031` | 클라이언트 정보 없음 |
| `4032` | API Key 유효하지 않음 |

### 예정

OAuth 2.0 Client Credentials 방식은 아직 제공하지 않습니다.
현재 운영 인증 방식은 API Key 헤더입니다.
