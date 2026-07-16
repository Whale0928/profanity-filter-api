# 7. SSO 사용자 소유 API Key 원장 전환

## Status
채택 (2026.07.17)

## Context
기존 `clients`는 사용자 정보, API Key 원문, 권한과 사용량을 한 행에 저장한다. API Key 관리 API도 제출된 API Key 자체로 인증하거나 이메일 인증 코드로 원문을 복구해, SSO 사용자가 자신의 여러 키를 안전하게 관리하는 구조로 확장하기 어렵다.

## Decision
`api_keys`를 API Key의 유일한 원장으로 사용하고 `clients`와 `/api/v1/clients/**`를 제거한다.

- V4 migration에서 모든 `clients` 행을 같은 ID의 `api_keys`로 복제한다.
- `legacy_client_id` 같은 중간 연결 컬럼은 두지 않는다.
- API Key 원문은 저장하지 않고 SHA-256 hash와 화면 표시용 `key_hint`만 저장한다.
- 기존 `records.api_key`도 hash로 전환해 요청 기록에 원문을 남기지 않는다.
- 기존 키는 migration 시 소유자를 추측하지 않고 `user_id`를 비워 둔다.
- SSO 로그인 완료 후 검증된 primary email과 같은 미이관 키를 비동기 작업으로 현재 사용자에게 연결한다.
- 연결 쿼리는 `user_id IS NULL`인 행만 갱신하므로 재로그인과 동시 실행에서 멱등성을 유지한다.
- 신규 발급 이메일은 요청값을 받지 않고 Login JWT 사용자의 primary email로 고정한다.
- 한 사용자가 여러 활성 API Key를 용도별로 가질 수 있다.
- 키 원문은 발급·재발행 성공 응답에서만 한 번 반환한다.
- 재발행은 기존 키를 만료하고 새 행을 생성하며, 만료는 기존 행의 만료 시각을 유지하는 멱등 작업이다.
- 관리 API는 `/api/v1/dashboard/keys` 아래에 두고 Login JWT만 허용한다.

기존 `client_reports`와 `request_count` 집계는 `api_keys` 기준으로 동작을 유지한다. 다만 해당 모델과 scheduler에는 `@Deprecated(forRemoval = true)`를 선언하고 신규 기능이 의존하지 않게 한다. 두 scheduler의 ShedLock 이름은 분리하며 향후 수집 중단 여부를 별도 결정한다.

## Consequences
- 기존 API Key는 값 변경 없이 계속 외부 API 인증에 사용할 수 있다.
- API Key와 요청 기록에서 원문 저장이 제거된다.
- 로그인 사용자는 기존 이메일의 키를 별도 복구 코드 없이 목록에서 확인할 수 있다.
- provider가 검증하고 서비스가 신뢰하는 primary email을 소유권 근거로 사용하므로 ADR 0005의 수동 claim 결정을 변경한다.
- 비동기 연결 직후 짧은 eventual consistency 구간이 생길 수 있지만 이후 로그인에서는 갱신할 행이 없다.
- 기존 공개 발급·이메일 복구·API Key 자체 인증 관리 API는 호환되지 않는다.
- 사용량 집계 중단 시 deprecated reporting 계층과 `client_reports`, `request_count`를 함께 제거해야 한다.

## Alternatives
- `legacy_client_id` 보존: 동일 ID 복제로 충분하며 중복 식별자를 만들 이유가 없어 제외한다.
- 로그인 요청에서 동기 연결: 목록 가시성은 즉시 보장하지만 OAuth 완료 응답을 DB 이관 작업에 결합하므로 제외한다.
- `clients` fallback 조회: migration 이후 원장이 두 개가 되고 만료된 키가 fallback으로 다시 인증될 수 있어 제외한다.
- 기존 이메일 인증 claim 유지: 추가 사용자 입력과 API Key 원문 복구 계약을 유지해야 하므로 제외한다.
