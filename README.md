# 한국어 비속어 필터 API

Aho-Corasick 알고리즘을 기반으로 한국어 비속어를 검출하고 마스킹하는 REST API입니다. `QUICK`, `NORMAL`, `FILTER` 모드를 지원하며 API Key 인증으로 사용할 수 있습니다.

## 바로가기

| 구분 | URL |
|---|---|
| 개발자 포털 | [https://developers.kr-filter.com](https://developers.kr-filter.com) |
| API 기준 주소 | `https://api.kr-filter.com` |
| API 문서 | [https://developers.kr-filter.com/docs](https://developers.kr-filter.com/docs) |
| OpenAPI JSON | [https://api.kr-filter.com/openapi.json](https://api.kr-filter.com/openapi.json) |
| API 개요 | [https://api.kr-filter.com/overview.md](https://api.kr-filter.com/overview.md) |
| LLM 문서 | [https://api.kr-filter.com/llms.txt](https://api.kr-filter.com/llms.txt) |
| 상태 확인 | [health](https://api.kr-filter.com/api/v1/health) · [ping](https://api.kr-filter.com/api/v1/ping) |

`api.profanity.kr-filter.com`은 레거시 주소이며 신규 연동에서는 사용하지 않습니다.

## 인증 모델

외부 API는 현재 `x-api-key: {API_KEY}`로 인증합니다. OAuth2 Client Credentials Bearer token은 준비 중이며 아직 사용할 수 없습니다.

API Key는 SSO 로그인 후 개발자 포털에서 발급·관리합니다. 기존 키는 값 변경 없이 계속 사용할 수 있으며, 같은 검증 이메일로 로그인하면 자동으로 계정에 연결됩니다.

외부 API에 아직 지원하지 않는 Bearer token을 보내면 HTTP `401`, business code `4017`로 거부됩니다. 로그인 JWT를 외부 API Key 대신 사용할 수도 없습니다.

## 빠른 시작

```bash
curl --request POST 'https://api.kr-filter.com/api/v1/filter' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --header 'x-api-key: YOUR_API_KEY' \
  --data '{
    "text": "검사할 문장",
    "mode": "FILTER"
  }'
```

필터링 모드는 다음과 같습니다.

| 모드 | 동작 |
|---|---|
| `QUICK` | 첫 번째 매칭을 빠르게 확인합니다. |
| `NORMAL` | 매칭된 비속어를 모두 반환합니다. |
| `FILTER` | 매칭된 비속어를 `*`로 마스킹합니다. |

`text`와 `mode`는 필수이며, 비동기 결과가 필요하면 `callbackUrl`을 추가할 수 있습니다.

## API 목록

아래 목록은 현재 운영 OpenAPI와 서버 보안 정책을 기준으로 합니다. 요청·응답 스키마와 실행 가능한 예제는 [개발자 포털 API 문서](https://developers.kr-filter.com/docs)에서 확인할 수 있습니다.

### 공개 및 문서

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/openapi.json` | OpenAPI 문서 |
| `GET` | `/overview.md` | API 개요, 인증 및 오류 모델 |
| `GET` | `/llms.txt` | LLM용 문서 인덱스 |
| `GET` | `/api/v1/health` | 애플리케이션 상태 확인 |
| `GET` | `/api/v1/ping` | 연결 확인 |

### 비속어 필터

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/api/v1/filter` | 비속어 필터링 요청 | API Key |
| `POST` | `/api/v1/filter/advanced` | 단일 단어 기반 고급 마스킹 | API Key |

### 단어 변경 요청

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/api/v1/word/request` | 비속어 단어 변경 요청 | API Key |

## 응답과 오류

대부분의 비즈니스 결과는 HTTP status와 함께 응답 본문의 `status.code`로도 전달됩니다. 전체 code와 오류 조건은 [Overview의 Error Model](https://api.kr-filter.com/overview.md)에서 관리합니다.

```json
{
  "trackingId": "bee20667-aa5a-4d39-94f5-0f2dcbd51cac",
  "status": {
    "code": 2000,
    "message": "Ok"
  },
  "detected": [],
  "filtered": "검사 결과",
  "elapsed": "0.07676 ms"
}
```

## 로컬 개발

요구 환경은 Java 21, Node.js 22, Docker입니다.

```bash
# 백엔드 검증
./gradlew staticCheck unitTest supportTest apiE2eTest

# 백엔드 실행
./gradlew :profanity-api:bootRun

# 프론트엔드 실행
npm --prefix ui/app ci
npm --prefix ui/app run dev

# 프론트엔드 검증
npm --prefix ui/app run typecheck
npm --prefix ui/app run build
```

기본 로컬 주소는 API `http://localhost:8080`, UI `http://localhost:5173`입니다.

## 배포

- 백엔드는 GitHub Release 발행 시 ARM64 이미지를 빌드해 `docker-registry.bottle-note.com/profanity-api`로 push합니다.
- 프론트엔드는 `main`의 `ui/app/**` 변경 시 CI를 통과한 뒤 ARM64 이미지를 `docker-registry.bottle-note.com/profanity-ui`로 push합니다.
- ArgoCD Image Updater가 새 semver 이미지를 감지하고 ArgoCD가 K3s 운영 환경에 자동으로 롤아웃합니다.
- 운영 manifest는 [deploy/overlays/production](deploy/overlays/production)에서 관리합니다.

## 이용 시 주의사항

- 무료 개인 운영 서비스이므로 100% 가용성을 보장하지 않습니다.
- 과도한 호출에는 rate limiting이 적용될 수 있습니다.
- 필터 결과가 모든 문맥과 표현을 완벽하게 판별한다고 보장하지 않습니다.
- 상업적 서비스에는 운영 요구사항과 결과 정확도를 별도로 검토해야 합니다.

문의와 오류 제보는 GitHub Issue를 이용해 주세요.
