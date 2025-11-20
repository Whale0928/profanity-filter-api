# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개발 환경 명령어

### 빌드 및 실행
```bash
# 프로젝트 빌드
./gradlew clean build

# 특정 모듈 빌드
./gradlew :profanity-api:build
./gradlew :profanity-domain:build
./gradlew :profanity-shared:build
./gradlew :profanity-storage:rdb:build
./gradlew :profanity-storage:redis:build

# 애플리케이션 실행 (개발 환경)
./gradlew :profanity-api:bootRun

# Docker 빌드 및 실행
docker-compose build
docker-compose up -d

# Blue-Green 배포 스크립트
./script/deploy.sh
./script/blue-green-swap.sh
./script/check_environment.sh
```

### 테스트
```bash
# 전체 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :profanity-api:test
./gradlew :profanity-domain:test

# RestDocs 생성 (API 문서 생성)
./gradlew :profanity-api:restdocs
./gradlew :profanity-api:generateRestDocs

# 특정 테스트 클래스 실행 예시
./gradlew test --tests "*.ProfanityControllerTest"
./gradlew test --tests "*.NormalProfanityFilterTest"
```

### 코드 커버리지
```bash
# 전체 프로젝트 커버리지 리포트 생성 (모듈별 + 통합)
./gradlew test jacocoRootReport checkOverallCoverageTarget

# 특정 모듈 커버리지 리포트 생성
./gradlew :profanity-api:jacocoTestReport
./gradlew :profanity-domain:jacocoTestReport

# 모듈별 커버리지 목표치 확인
./gradlew :profanity-api:checkCoverageTargets
./gradlew :profanity-domain:checkCoverageTargets

# 커버리지 리포트 위치
# - 통합 리포트: build/reports/jacoco/jacocoRootReport/html/index.html
# - 모듈별 리포트: {module}/build/reports/jacoco/test/html/index.html
```

#### 커버리지 목표치
- **profanity-api**: 70% (Line), 65% (Branch), 70% (Instruction)
- **profanity-domain**: 80% (Line), 75% (Branch), 80% (Instruction)
- **profanity-shared**: 60% (Line), 55% (Branch), 60% (Instruction)
- **profanity-storage:rdb**: 70% (Line), 65% (Branch), 70% (Instruction)
- **profanity-storage:redis**: 70% (Line), 65% (Branch), 70% (Instruction)
- **전체 프로젝트**: 75% (Line), 70% (Branch), 75% (Instruction)

*참고: 목표치는 현재 빌드 실패를 유발하지 않으며, 달성 여부만 체크합니다.*

### 의존성 관리
```bash
# 의존성 확인
./gradlew dependencies

# 의존성 업데이트 확인
./gradlew dependencyUpdates
```

## 프로젝트 아키텍처

### 멀티모듈 구조
프로젝트는 클린 아키텍처 원칙을 따르는 멀티모듈 구조로 설계:

- **profanity-api**: 프레젠테이션 계층
  - REST API 컨트롤러 (`ClientsController`, `ProfanityController`, `WordManagementController`)
  - Spring Security 인증/인가 (`CustomAuthenticationFilter`, `ClientVerificationAspect`)
  - 이메일 서비스 (`EmailService`)
  - 예외 처리 (`GlobalExceptionHandler`)

- **profanity-domain**: 비즈니스 로직 계층
  - 핵심 비속어 필터링 엔진 (`AhocorasickFilter`, `NormalProfanityFilter`)
  - 클라이언트 관리 (`ClientsCommandService`, `ClientMetadataReader`)
  - 이벤트 처리 (`AsyncFilterEvent`, `FilterEventHandler`)
  - 단어 관리 및 동기화 (`WordManagementService`, `SyncScheduler`)
  - 일일 리포트 생성 (`DailyReportScheduler`)

- **profanity-shared**: 공통 데이터 모델 및 유틸리티
  - API 응답 모델 (`ApiResponse`, `FilterApiResponse`, `Status`)
  - 상수 및 열거형 (`Mode`, `StatusCode`)
  - 성능 측정 (`Elapsed`, `ElapsedStartAt`)

- **profanity-storage**: 데이터 접근 계층
  - **rdb**: JPA 리포지토리 구현
  - **redis**: Redis 캐싱 설정

### 주요 기능 흐름

1. **비속어 필터링 요청 처리**:
   - `ProfanityController` → `CustomAuthenticationFilter` (API Key 인증)
   - → `ProfanityHandler` → `AhocorasickFilter` (아호코라식 알고리즘)
   - → 응답 반환 (검출된 비속어 목록 및 필터링된 텍스트)

2. **클라이언트 관리**:
   - API Key 발급: `ClientsCommandService.register()` → `APIKeyGenerator`
   - 이메일 인증: `EmailService` → 템플릿 기반 이메일 발송
   - 권한 검증: `ClientVerificationAspect` → `@VerifiedClientOnly` 어노테이션 처리

3. **데이터 동기화**:
   - `SyncScheduler` → 중앙 DB에서 비속어 목록 주기적 동기화
   - `DailyReportScheduler` → 클라이언트별 일일 사용량 리포트 생성

### 환경별 설정

- **개발 환경 (dev)**: SQL 로깅 활성화, 디버그 모드
- **운영 환경 (prod)**: Blue-Green 배포, Redis 캐싱, 성능 최적화
- **프로파일 구조**: `application.yml` + 모듈별 설정 (`application-rdb.yml`, `application-redis.yml`, `application-domain.yml`, `application-shared.yml`)

### API 엔드포인트

- `/api/v1/filter/` - 비속어 필터링 (POST)
- `/api/v1/clients/register` - 클라이언트 등록
- `/api/v1/clients/verify-email` - 이메일 인증
- `/api/v1/clients/info` - 클라이언트 정보 조회
- `/api/v1/words/` - 비속어 단어 관리
- `/api/v1/health`, `/api/v1/ping` - 헬스체크

### 보안 및 인증

- API Key 기반 인증 (`x-api-key` 헤더)
- Spring Security 커스텀 필터 체인
- 이메일 인증을 통한 클라이언트 검증
- Rate limiting 구현 (클라이언트별 요청 제한)