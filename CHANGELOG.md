# Changelog

이 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르고,
이 프로젝트는 [유의적 버전](https://semver.org/lang/ko/)을 따릅니다.
버전은 운영에 배포되는 컨테이너 이미지 태그(`profanity-api:v{semver}`)를 기준으로 합니다.

## [Unreleased]

### Added
- ADR(아키텍처 결정 기록) 도입 (`docs/adr/`)

## [0.0.5] (2026.06.20)

### Changed
- 필터 요청 로그를 요청당 8줄에서 구조화된 2줄(`[FILTER] 요청 수신` / `[FILTER] 처리 결과`)로 축소

### Fixed
- 필터링 지연 시간 로그의 `µsms` 단위 표기 오류 수정

### Security
- 로그에 평문으로 노출되던 API 키를 앞 4자만 남기고 마스킹(`1F8L****`)

## [0.0.4] (2026.06.20)

### Added
- 신규 운영 도메인 `api.kr-filter.com`(Cloudflare proxied)
- `CF-Connecting-IP` 기반 실제 클라이언트 IP 보존(신규 도메인 경로)
- 응답 본문 `meta` 커스터마이징(호스트 기반 `servedVia`)

### Changed
- API 파드 리소스 상향: limits 4Gi/4 CPU, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0`
- 헬스 프로브 경로를 `/api/v1/health`로 통일

### Removed
- actuator / micrometer 등 관측 스택 제거
