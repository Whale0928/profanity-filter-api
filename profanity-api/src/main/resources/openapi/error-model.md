# Error Model

### 응답 정책

OpenAPI의 `responses`는 HTTP 상태 코드를 기준으로 표시합니다.
이 API는 대부분의 실패를 HTTP `200`과 본문 `status.code`로 반환하므로, 각 operation에 HTTP `400`이나 `404`가 따로 표시되지 않을 수 있습니다.
기존 API Key 실패 계약은 HTTP `200`과 본문 code를 유지합니다. 새 로그인 인증 API와 credential 충돌은 실제 HTTP `400`/`401`/`403`과 해당 본문 code를 함께 반환합니다.

### 오류 응답 형식

| API    | 오류 형식                                                  |
|--------|--------------------------------------------------------|
| 일반 API | `ApiResponse<T>`에서 `data = null`                       |
| 필터 API | `FilterApiResponse`에서 `detected = []`, `filtered = ""` |

상세 원인은 `status.DetailDescription`에서 확인합니다.

### 상태 코드

| Code   | Message                 | 설명                       |
|--------|-------------------------|--------------------------|
| `2000` | `Ok`                    | 정상 처리                    |
| `2020` | `Accepted`              | 비동기 요청 접수                |
| `2021` | `Processing`            | 처리 진행 중                  |
| `4000` | `Bad_request`           | 요청 형식 오류, 필수 값 누락, 검증 실패 |
| `4001` | `Invalid_callback_url`  | callback URL 형식 오류       |
| `4002` | `Invalid_tracking_id`   | 유효하지 않은 tracking ID      |
| `4003` | `Not_fount_tracking_id` | tracking ID를 찾을 수 없음     |
| `4004` | `Ambiguous_credentials` | 다중 또는 중복 인증 정보 제출       |
| `4010` | `Unauthorized`          | API Key 누락               |
| `4011` | `Oauth2_login_failed`   | OAuth2 로그인 실패          |
| `4012` | `Login_code_invalid`    | 로그인 교환 코드 오류, 만료 또는 재사용  |
| `4013` | `Login_token_invalid`   | 로그인 access token 검증 실패   |
| `4014` | `Login_token_expired`   | 로그인 access token 만료       |
| `4015` | `Refresh_token_invalid` | refresh token/session 오류 또는 만료 |
| `4016` | `Refresh_token_reused`  | 소비된 refresh token 재사용     |
| `4017` | `Oauth2_access_token_unsupported` | 외부 API OAuth2 access token 미지원 |
| `4030` | `Forbidden`             | 권한 부족 또는 차단된 클라이언트       |
| `4031` | `Not_found_client`      | 클라이언트 정보 없음              |
| `4032` | `Invalid_api_key`       | 유효하지 않은 API Key          |
| `4033` | `User_inactive`         | 로그인 사용자 비활성 상태          |
| `4290` | `Too_many_requests`     | 요청 제한 초과                 |
| `5000` | `Internal_server_error` | 서버 내부 오류                 |
| `5030` | `Service_unavailable`   | 서비스 점검 또는 일시 사용 불가       |
