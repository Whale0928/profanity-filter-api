# Error Model

### 응답 정책

OpenAPI의 `responses`는 HTTP 상태 코드를 기준으로 표시합니다.
이 API는 대부분의 실패를 HTTP `200`과 본문 `status.code`로 반환하므로, 각 operation에 HTTP `400`이나 `404`가 따로 표시되지 않을 수 있습니다.
API Key 누락처럼 보안 필터에서 차단되는 요청은 HTTP `401`과 `status.code = 4010`이 함께 반환될 수 있습니다.

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
| `4010` | `Unauthorized`          | API Key 누락               |
| `4011` | `Oauth2_login_failed`   | OAuth2 로그인 실패          |
| `4030` | `Forbidden`             | 권한 부족 또는 차단된 클라이언트       |
| `4031` | `Not_found_client`      | 클라이언트 정보 없음              |
| `4032` | `Invalid_api_key`       | 유효하지 않은 API Key          |
| `4290` | `Too_many_requests`     | 요청 제한 초과                 |
| `5000` | `Internal_server_error` | 서버 내부 오류                 |
| `5030` | `Service_unavailable`   | 서비스 점검 또는 일시 사용 불가       |
