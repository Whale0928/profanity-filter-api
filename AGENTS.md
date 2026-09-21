# AGENTS.md

이 저장소에서 작업하는 코딩 에이전트를 위한 지침. 도구별 사본을 두지 않고 이 파일 하나만 관리한다. Claude Code는 `AGENTS.md`를 자동으로 읽지 않으므로 로컬에서 `ln -s AGENTS.md CLAUDE.md`로 링크를 만들어 쓴다(링크는 `.gitignore` 대상).

## 프로젝트 개요

Aho-Corasick 알고리즘 기반의 한국어 비속어 필터링 REST API 서비스. API Key 인증 후 동기/비동기 필터링을 제공하며, KISO 이용자 보호 시스템 API와 유사한 스펙으로 구현됨.

- **기술 스택**: Java 21, Spring Boot 3.4.0, Spring Data JPA, Redis, MySQL(운영·로컬). 스키마는 Flyway(`profanity-storage/rdb/.../db/migration`)로 관리하고 JPA는 `ddl-auto: validate`. 테스트 DB는 H2가 아니라 Testcontainers `mysql:8.4`
- **프런트엔드**: `ui/app` — React 19 + Vite 7 + TypeScript. 개발자 포털과 관리자 콘솔을 함께 담으며 API와 **별도 이미지로 배포**
- **빌드**: Gradle (Wrapper 8.8), 멀티모듈, `io.spring.dependency-management` 1.1.6
- **아키텍처**: Clean Architecture 기반 멀티모듈 (도메인이 storage port를 정의하고 storage 모듈이 구현)
- **배포**: K3s + ArgoCD GitOps (`deploy/overlays/production`), Zot 레지스트리 + Image Updater 자동 롤아웃
- **운영 도메인**: `api.kr-filter.com`(Cloudflare proxied, 권장) / ~~`api.profanity.kr-filter.com`~~(DNS-only, 레거시·지원 종료 예정) / 문서: `/openapi.json`, `/overview.md`, `/llms.txt`
- **UI 도메인**: `developers.kr-filter.com` (`profanity-ui` Deployment)
- **version/group**: `0.0.1-SNAPSHOT` / `app.profanity-filter`

## 멀티모듈 구조

`settings.gradle` 기준 6개 모듈. `profanity-api`만 실행 가능한 Boot JAR이며, 나머지는 라이브러리 JAR(`bootJar` 비활성, `jar` 활성).

```
profanity-api (Presentation, Boot JAR)   ── domain + storage:rdb + storage:redis 의존
  ├─ presentation/  REST Controllers
  ├─ security/      API Key / 로그인 JWT 인증, SSO handler, refresh cookie·CSRF
  ├─ application/   로그인 orchestration, API Key 소유권 연결, Async 이벤트 리스너, HttpClient
  ├─ web/response/  응답 meta 커스터마이징 (ResponseBodyAdvice 기반)
  ├─ dto/           요청 DTO (관리자 요청은 dto/request/admin)
  ├─ openapi/       Swagger 합성 어노테이션. 컨트롤러는 @Hidden 외 Swagger 어노테이션을 직접 쓰지 않음(OpenApiArchitectureTest가 강제)
  ├─ exception/     GlobalExceptionHandler
  └─ config/        Aspect / LocalCache(Caffeine) / Mail

profanity-domain (Business Logic, 라이브러리)   ── shared 를 api() 로 재노출
  ├─ application/filter/   NormalProfanityFilter(Aho-Corasick), DefaultProfanityHandler
  ├─ application/manage/    SyncScheduler, DailyReportScheduler, Word/Report/Sync 서비스
  ├─ application/apikey/    API Key 발급·재발행·만료·소유권 연결
  ├─ application/client/    APIKeyGenerator
  ├─ application/auth/      SSO 계정 upsert, 교환 코드, refresh session rotation
  ├─ application/event/     FilterEvent / AsyncFilterEvent / TrackingRecorder
  ├─ application/admin/     관리자 사전·사용자·API Key 관리, 감사 기록, 통계
  ├─ application/inquiry/   문의 등록·답변, 단어 요청 승인·거절
  ├─ application/news/      소식 작성·게시
  └─ domain/                엔티티(ApiKey, User/OAuthAccount, LoginSession, ProfanityWord, Records, Inquiry, NewsPost, AdminAuditLog 등) + Repository 포트

profanity-storage:rdb (Data Access - RDB)
  └─ domain 의 Repository 포트를 Spring Data JPA(Jpa*Repository)로 구현

profanity-storage:redis (Data Access - Cache)
  └─ RedisConfig / RedisTemplate / properties

profanity-shared (Common)
  └─ ApiResponse / status code / elapsed / WebConfig / Mode 등 공통 응답·상수

profanity-test-support (Test Support)
  └─ MySqlTestContainer(mysql:8.4 + Flyway), seed fixture, Fake*, RecordProbe 등 E2E 공용 지원
```

Gradle 모듈 밖에는 `ui/`(프런트엔드), `adr/`(아키텍처 결정 기록), `deploy/`(GitOps 매니페스트), `http/`(요청 예시)가 있음.

의존 방향: `api → domain → shared`, `api → storage:rdb → domain`, `api → storage:redis → shared`.

## 핵심 컴포넌트

### NormalProfanityFilter (`profanity-domain/.../application/filter`)
- `org.ahocorasick:ahocorasick:0.6.3` 기반 다중 패턴 매칭 (O(n+m))
- `private static volatile Trie trie` — 동기화 시 새 Trie를 빌드해 원자적 재할당(Lock-free read)
- `Trie.builder().ignoreOverlaps().ignoreCase()`로 빌드
- 입력 정규화: `[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]` 제거 후 매칭

### 필터링 모드 (`Mode` enum, profanity-shared)
- `QUICK`: 첫 매칭만 반환 (`firstMatched`)
- `NORMAL`: 모든 매칭 반환 (`allMatched`)
- `FILTER`: 모든 매칭을 `*`로 마스킹 (`sanitizeProfanity`)
- `@JsonCreator`로 대소문자 무시 파싱

### 비동기 처리 (`DefaultProfanityHandler`)
- 요청에 `callbackUrl` 존재 시: 즉시 ACCEPTED 응답 → `CompletableFuture`로 필터링 → `AsyncFilterEvent` 발행
- `AsyncFilterEventListener`(api 모듈, `@Async @EventListener`)가 `RestClient`로 콜백 URL에 POST (재시도 없음)

### 요청 기록 (`FilterEvent` / `TrackingRecorder`)
- 동기 필터링 후 `FilterEvent` 발행 → `records` 저장 (trackingId, mode, apiKeyHash, 요청문, 검출 단어, referrer, ip)
- 클라이언트 IP는 `HttpClient.getClientIP`가 추출: `CF-Connecting-IP` → `X-Forwarded-For`(첫 IP) → 폴백 헤더 → `getRemoteAddr` 순. Cloudflare proxied 경로(`api.kr-filter.com`)에서만 실제 IP가 기록되고, DNS-only 경로(레거시 도메인)에서는 klipper-lb L4 SNAT로 인해 k3s 내부망 IP(`10.42.x`)가 기록됨
- `@Cacheable`에 적중한 요청은 핸들러를 거치지 않아 `records`에 남지 않음. `records` 건수는 실제 호출 수가 아니라 기록된 요청 수
- `request_text`에는 요청 원문이 그대로 저장됨(`varchar(255)`, 보관 기간 정책 없음). 개인정보가 섞여 들어올 수 있으므로 원문을 출력하거나 외부로 내보내는 작업은 주의

### 응답 커스터마이징 (`profanity-api/.../web/response`)
- `ResponseCustomizingAdvice`(`@RestControllerAdvice` + `ResponseBodyAdvice`)가 직렬화 직전 응답 `meta`(Map)에 컨텍스트 정보 주입
- `ResponseCustomizer` 인터페이스 + `HostResponseCustomizer`(요청 호스트가 `app.response.proxied-host` 설정값과 일치하면 `servedVia` 추가)
- `meta`는 `@JsonInclude(NON_EMPTY)`라 비어 있으면 직렬화에서 제외(기존 응답 불변 = 하위호환)
- **[갭] 적용 조건이 `body instanceof ApiResponse` 인데, 메인 필터 엔드포인트는 `FilterApiResponse`(별도 record, `ApiResponse` 아님)를 반환하므로 `meta`가 붙지 않음.** 현재 `ApiResponse` 반환 경로에만 적용됨

### 인증 체계 (`profanity-api/.../security`)
- Stateless Spring Security에서 `API_KEY`, `LOGIN_JWT`, 미래 확장용 `OAUTH2_ACCESS_TOKEN`을 명시적으로 분리
- `CustomAuthenticationFilter` → `RequestCredentialResolver` → 타입별 authenticator가 정확히 하나의 `Authentication`만 새 `SecurityContext`에 설정
- 기존 외부 API는 `X-API-KEY`와 `AUTH_API_KEY`; `/api/v1/auth/me`, `/api/v1/dashboard/**`는 RS256 로그인 JWT와 `AUTH_LOGIN_JWT`/`ROLE_USER` 사용
- `api_keys`가 API Key 인증의 유일한 원장이며 원문 대신 SHA-256 hash만 저장
- OAuth2 Client Credentials access token은 의도적으로 미구현. 외부 API Bearer는 `OAUTH2_ACCESS_TOKEN` 경계에서 HTTP 401/code 4017로 fail-closed
- SSO 성공은 일회용 교환 코드 → `/api/v1/auth/exchange`; access token 15분, opaque refresh 14일/절대 세션 30일, MySQL hash 저장과 rotation 사용
- refresh replay는 5초 grace 안에서 loser 요청만 실패하고 family를 유지하며, grace 이후 재사용은 session family 전체 폐기
- `ExcludePath` enum으로 public/자체 검증 경로를 관리하고 refresh는 HttpOnly cookie와 CSRF로 보호
- `@VerifiedClientOnly` + `ClientVerificationAspect`(`@Around @Order(1)`): BLOCK/DISCARD 권한 클라이언트를 403으로 차단
- 권한(`PermissionsType`): READ / WRITE / DELETE / BLOCK / DISCARD (기본 [READ])

### 스케줄러 (`profanity-domain/.../application/manage`)
- `SyncScheduler`: `@Scheduled(fixedDelay = 60000)` — 1분마다 DB 단어 수 비교, 변경 시에만 Trie 재동기화. (`@SchedulerLock`은 주석 처리되어 미적용)
- `DailyReportScheduler`: 매일 01:00에 `api_keys.request_count`와 `client_reports`를 집계한다. 두 경로는 수집 중단 검토 대상으로 `@Deprecated(forRemoval = true)`이며 서로 다른 ShedLock을 사용한다.

### 관리자 통계 (`profanity-domain/.../application/admin/AdminStatisticsService`)
- `GET /api/v1/admin/statistics?days=7|30|90` 하나로 요약·일별 추이·모드별 분포·운영 현황·API Key 상위 5를 반환. 그 밖의 `days`는 `BAD_REQUEST`로 거절
- 원본은 `records`. 수집 중단 예정인 `client_reports`와 `api_keys.request_count`에는 의존하지 않음
- 기간 조회는 V6의 `idx_records_created (created_at, id)`를 사용
- 일별 집계만 네이티브 질의(`DATEDIFF(created_at, :from)`). 날짜를 잘라내지 않고 기간 시작 경계로부터의 날짜 차이로 나눠, 저장된 값의 시간대를 해석하지 않음

### 시각 저장 규약 (시각을 다루기 전에 반드시 확인)
- `Instant` 컬럼(`users`, `inquiries`, `inquiry_replies`, `admin_audit_logs`, `news_posts`, `profanity_word`, 로그인 세션)은 UTC
- `LocalDateTime` 컬럼(`records.created_at`, `api_keys.issued_at`·`expired_at`·`last_used_at`, `word_management.requested_at`, `client_reports.created_at`)은 Asia/Seoul 벽시계
- JDBC URL이 `serverTimezone=Asia/Seoul`이라 드라이버가 `LocalDateTime`을 JVM 시간대에서 Asia/Seoul로 변환해 보냄. **운영 JVM은 `TZ=Asia/Seoul`과 `-Duser.timezone=Asia/Seoul`로 고정**해 변환량을 0으로 둠(`deployment.yaml`). 이 설정을 지우면 코드가 KST로 만든 시각에 9시간이 더 얹힘. 실제로 2026.02~09에 `api_keys` 시각이 밀렸고 2026.09.21에 보정함
- DB에서 직접 조회하면 `NOW()`는 UTC이고 `LocalDateTime` 컬럼은 KST라 9시간 차이가 나는 것이 정상

## REST 엔드포인트 (`profanity-api/.../presentation`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/filter` (JSON) | 동기/비동기 필터링, `@Cacheable` |
| POST | `/api/v1/filter` (form-urlencoded) | 동기 필터링 |
| POST | `/api/v1/filter/advanced` | 단일 word 마스킹 |
| GET | `/api/v1/dashboard/keys` | 로그인 사용자의 API Key 목록 조회 |
| POST | `/api/v1/dashboard/keys` | API Key 발급 |
| POST | `/api/v1/dashboard/keys/{apiKeyId}/reissue` | API Key 재발급 |
| DELETE | `/api/v1/dashboard/keys/{apiKeyId}` | API Key 만료 |
| POST | `/api/v1/word/request` | 단어 추가/제거/수정 요청 |
| POST | `/api/v1/word/accept/{requestId}` | 단어 요청 승인 (WRITE 권한) |
| GET | `/api/v1/sync?password=...` | 수동 동기화 (관리자) |
| GET | `/api/v1/health`, `/api/v1/ping` | 헬스 체크 |
| POST | `/api/v1/auth/exchange` | SSO 일회용 코드를 access/refresh token으로 교환 |
| GET | `/api/v1/auth/csrf` | refresh 요청용 CSRF token 조회 |
| POST | `/api/v1/auth/refresh` | refresh token rotation |
| GET | `/api/v1/auth/me` | LOGIN_JWT 사용자 조회 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| GET·POST | `/api/v1/dashboard/inquiries`, GET `/{inquiryId}` | 로그인 사용자의 문의 조회·등록 |
| GET | `/api/v1/news`, `/api/v1/news/{id}` | 공개 소식 조회 |
| GET·POST·PUT | `/api/v1/admin/words`, PUT `/{wordId}` | 관리자 사전 조회·등록·수정 |
| GET·PATCH·POST | `/api/v1/admin/inquiries`, `/{inquiryId}`, `/{inquiryId}/status`·`/replies`·`/word-decision` | 관리자 문의 처리와 단어 요청 승인·거절 |
| GET·PATCH | `/api/v1/admin/users`, PATCH `/{userId}/status` | 관리자 사용자 조회·상태 변경 |
| GET·POST | `/api/v1/admin/keys`, POST `/{apiKeyId}/revoke` | 관리자 API Key 조회·폐기 |
| GET·POST·PUT·DELETE | `/api/v1/admin/news`, `/{newsId}` | 관리자 소식 관리 |
| GET | `/api/v1/admin/statistics?days=` | 관리자 통계 개요 |

- `/api/v1/admin/**`은 로그인 JWT와 ADMIN 역할을 요구하며 `@Hidden`으로 OpenAPI 문서에서 제외됨
- 응답은 대부분 HTTP 200이며, 비즈니스 결과는 `status.code`로 전달. 전체 코드는 `StatusCode` enum과 `/overview.md`의 Error Model 기준

## 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 로컬 실행
./gradlew :profanity-api:bootRun

# 테스트 (루트 test가 전 모듈 test에 의존. E2E가 포함되어 Docker 필요)
./gradlew test

# CI와 같은 단계별 검증
./gradlew staticCheck   # 포맷 + 컴파일, 테스트 없음
./gradlew unitTest      # Docker 없이 도는 단위·슬라이스 테스트
./gradlew supportTest   # profanity-test-support 검증 (Docker 필요)
./gradlew apiE2eTest    # API E2E (Docker 필요)

# 커버리지: 모듈별 리포트는 test 후 자동 생성(build/reports/jacoco/test/html)
# 전 모듈 통합 리포트는 jacocoRootReport(build/reports/jacoco/aggregate)
./gradlew jacocoRootReport

# 코드 포맷 (google-java-format)
./gradlew spotlessApply   # 적용 / spotlessCheck 검사

# OpenAPI JSON 확인
# 애플리케이션 기동 후 GET /openapi.json

# 로컬 기동 (Taskfile)
task api      # 백엔드 포그라운드 기동
task ui:up    # UI 개발 서버 (ui:down / ui:restart / ui:status / ui:logs)

# UI 검증
cd ui/app && npm run typecheck && npm run build
```

## 테스트 컨벤션

- JUnit 5(Platform), 한글 `@DisplayName` + `@Nested` BDD 스타일. API 스펙은 Springdoc 기반 `/openapi.json` 응답으로 검증
- 도메인 계층은 Mock보다 **테스트 더블 우선** — `Inmemory*Repository`, `Fake*` 등 실제 구현 사용. Mockito는 외부 의존 격리가 필요한 일부에만 제한적 사용
- E2E와 마이그레이션 테스트는 Testcontainers MySQL을 사용(`AbstractApiTester`, `MySqlTestContainer`)
- 도메인 Repository 포트에 메서드를 추가하면 포트를 직접 구현한 테스트 더블을 전부 고쳐야 컴파일됨. `profanity-domain/src/test/java/app/domain/InMemory*Repository` 외에 인증 테스트 안의 중첩 더블(`LoginJwtServiceTest`, `SsoAccountServiceTest` 등)도 포함
- 마이그레이션을 추가하면 `AdminPortalMigrationTest`의 적용 개수 검증을 함께 갱신
- (세부 작성 규칙은 별도 스킬로 관리)

## 배포 (K3s + ArgoCD GitOps)

- **GitOps 경로**: `deploy/application.yaml`(ArgoCD Application) → `deploy/overlays/production`(Kustomize, namespace `profanity-production`, automated prune/selfHeal)

### 이미지 빌드·배포 흐름

API와 UI는 **별도 이미지·별도 Deployment**다. 화면이 바뀌는 변경은 API만 배포해서는 반영되지 않으므로 UI도 따로 배포한다.

1. **API (`profanity-api`)**: GitHub Release를 publish하면 `release.yaml`이 릴리스 태그(`v{major}.{minor}.{patch}`)로 arm64 이미지를 빌드해 Zot 레지스트리 `docker-registry.bottle-note.com`에 push
2. **UI (`profanity-ui`)**: `ui-deploy.yml`을 수동 실행(workflow_dispatch). 타입 검사와 빌드를 거쳐 push하며 태그는 `v0.{run_number}.{run_attempt}`로 자동 부여
3. **자동 감지**: ArgoCD Image Updater(`default-updater`, ImageUpdater CRD)가 `^v[0-9]+\.[0-9]+\.[0-9]+$` 태그 중 최신을 ~5분 주기로 감지
   - write-back은 **ArgoCD live application spec의 kustomize image override에 직접 기록**한다. git/`deploy/.../kustomization.yaml`은 건드리지 않으며 그 `newTag`는 고정값(실제 운영 태그와 무관)
4. **롤아웃**: ArgoCD automated/selfHeal이 변경을 감지해 RollingUpdate 수행

- **수동 빌드(비상 경로)**: `docker buildx build --platform linux/arm64 -t docker-registry.bottle-note.com/profanity-api:v{semver} --load .` 후 `docker push`. UI는 빌드 컨텍스트가 `ui/app`. 운영 nodeSelector가 `arm64`라 **arm64 이미지 필수**
  - 감지가 태그 기준이라 **같은 태그를 덮어쓰면 롤아웃되지 않음**. 수동으로 매긴 UI 태그는 다음 `ui-deploy` 실행 번호와 겹칠 수 있고, 수동으로 올린 API 태그에는 대응하는 GitHub Release가 없음
- `deploy/` 매니페스트 변경은 main push만으로 ArgoCD가 반영하며 이미지가 그대로여도 pod가 재시작됨
- Flyway는 앱 기동 시 실행됨. readiness 90초·liveness 120초 지연 안에 끝나지 않는 DDL은 재시작 루프와 `flyway_schema_history` 실패 행을 남길 수 있으므로 큰 테이블의 DDL은 배포 전에 규모를 확인

- **Deployment**: replicas 2, `arm64` nodeSelector, RollingUpdate, port 8080, `SPRING_PROFILES_ACTIVE=prod`, 리소스 requests 1Gi/500m·limits 4Gi/4, `TZ=Asia/Seoul`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul`(시각 저장 규약 참고), liveness/readiness `/api/v1/health`. 시크릿은 ExternalSecret(1Password Connect)로 주입된 `profanity-secrets`
- **라우팅**: Gateway API `HTTPRoute`(`main-gateway`, envoy-gateway-system) → `api.kr-filter.com`·~~`api.profanity.kr-filter.com`~~ → `profanity-api:80`, `developers.kr-filter.com` → `profanity-ui`
- **클라이언트 IP 보존**: `api.kr-filter.com`은 Cloudflare proxied(`CF-Connecting-IP` 전달)라 실제 IP가 `records`에 기록됨. ~~`api.profanity.kr-filter.com`~~은 DNS-only(2-level 서브도메인이 Cloudflare 무료 Universal SSL 미지원)라 Cloudflare를 거치지 않아 klipper-lb L4 SNAT로 내부망 IP만 기록됨 → 지원 종료 예정이며 신규 도메인으로 이전 유도 필요
- **Redis(운영)**: `redis:7-alpine` 단일 Deployment, 영속성 없음(`--save ""`), `allkeys-lru` 캐시 전용
- **인프라 정의**: `module.platform` 서브모듈(ArgoCD, cert-manager, Envoy Gateway, external-secrets, image-updater, Zot). SOPS 암호화 시크릿 포함 — **값 열람·수정 금지**
- CI: `.github/workflows/ci.yml`이 ui 잡(타입 검사, 빌드, discovery 산출물 검증)과 backend 잡(`staticCheck` → `unitTest` → `supportTest` → `apiE2eTest`)을 실행. 배포는 `release.yaml`(API)과 `ui-deploy.yml`(UI)이 담당

## 문서

- 아키텍처 결정은 `adr/`에 표준 포맷(Status / Context / Decision / Consequences / Alternatives)으로 기록. 템플릿은 `adr/0000 ADR 템플릿.md`, 상태 값은 제안·승인·폐기·대체됨
- 파일명은 `NNNN 제목.md` 형식이며 문서 제목과 같게 유지. 결정이 바뀌면 기존 문서를 고쳐 쓰지 않고 새 ADR로 대체
- 별도 `docs/` 폴더는 두지 않음. 구현 전 계획도 상태가 제안인 ADR로 남김

## 코드 품질 및 주의사항

작업 전 반드시 인지해야 할 현 상태(추측 아님, 코드 확인 기반):

1. **version catalog 부분 사용**: `gradle/libs.versions.toml`은 Testcontainers·Flyway 버전과 commons-lang3, springdoc, archunit, swagger-annotations에 쓰임. 단 Guava는 catalog에 `33.0.0-jre`가 선언돼 있는데 root `build.gradle`이 `31.1-jre`를 직접 선언해 불일치. 의존성 변경 시 어느 쪽이 실제로 적용되는지 확인
2. **코드 품질 도구**: Spotless(google-java-format, root `build.gradle` subprojects 적용, `.pre-commit-config.yaml`이 `spotlessCheck` 실행)와 JaCoCo(`jacocoRootReport` 통합 리포트, Lombok 생성 코드는 `lombok.config`로 제외) 도입됨. **NullAway는 여전히 미적용**
3. **응답 `meta` 커스터마이징의 적용 범위**: `ApiResponse` 반환 경로에만 적용되고 메인 `/api/v1/filter`(`FilterApiResponse`)에는 미반영. 통일하려면 응답 래핑 재설계 필요
4. **CORS**: `SecurityConfig`가 경로별로 나눔. `/api/v1/auth/**`·`/dashboard/**`·`/admin/**`은 설정된 origin만 허용(credentials 허용), 그 밖의 외부 API는 `allowedOrigins(List.of("*"))`에 credentials 비허용
5. **`SyncScheduler` ShedLock 미적용**: 다중 인스턴스(replicas 2) 환경에서 중복 동기화 가능 (코드에 주석으로 인지됨)
6. **`NormalProfanityFilter.collect`**: `HashSet`이며 동기화 메서드 내 원자적 재할당으로만 수정됨
7. `@Cacheable` 캐시(Caffeine `request_filter`)는 TTL 24시간·최대 1,000건이고 키는 `문장 + 모드`뿐. 사전이 바뀌면 `FilterResultCacheEvictor`가 인스턴스별로 비움. 키에 고객 정보가 없으므로 고객마다 결과가 달라지는 기능을 넣을 때는 캐시부터 확인
8. 시크릿 파일(`.env`, `.secrets`, `*.enc.yaml`, `*.sops.yaml`, `module.secrets/`)의 값은 읽거나 출력하지 말 것
