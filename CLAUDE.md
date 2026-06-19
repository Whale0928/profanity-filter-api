# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Aho-Corasick 알고리즘 기반의 한국어 비속어 필터링 REST API 서비스. API Key 인증 후 동기/비동기 필터링을 제공하며, KISO 이용자 보호 시스템 API와 유사한 스펙으로 구현됨.

- **기술 스택**: Java 21, Spring Boot 3.4.0, Spring Data JPA, Redis, MySQL(운영) / H2(개발·테스트)
- **빌드**: Gradle (Wrapper 8.8), 멀티모듈, `io.spring.dependency-management` 1.1.6
- **아키텍처**: Clean Architecture 기반 멀티모듈 (도메인이 storage port를 정의하고 storage 모듈이 구현)
- **배포**: K3s + ArgoCD GitOps (`deploy/overlays/production`)
- **운영 도메인**: `api.profanity.kr-filter.com` / 문서: `whale0928.github.io/profanity-filter-api`
- **version/group**: `0.0.1-SNAPSHOT` / `app.profanity-filter`

## 멀티모듈 구조

`settings.gradle` 기준 5개 모듈. `profanity-api`만 실행 가능한 Boot JAR이며, 나머지는 라이브러리 JAR(`bootJar` 비활성, `jar` 활성).

```
profanity-api (Presentation, Boot JAR)   ── domain + storage:rdb + storage:redis 의존
  ├─ presentation/  REST Controllers
  ├─ security/      API Key 인증 (filter / authentication / aspect / annotation)
  ├─ application/   Async 이벤트 리스너, EmailService, HttpClient
  ├─ exception/     GlobalExceptionHandler
  └─ config/        Aspect / LocalCache(Caffeine) / Mail

profanity-domain (Business Logic, 라이브러리)   ── shared 를 api() 로 재노출
  ├─ application/filter/   NormalProfanityFilter(Aho-Corasick), DefaultProfanityHandler
  ├─ application/manage/    SyncScheduler, DailyReportScheduler, Word/Report/Sync 서비스
  ├─ application/client/    ClientsCommandService, MetadataReader, APIKeyGenerator
  ├─ application/event/     FilterEvent / AsyncFilterEvent / TrackingRecorder
  └─ domain/                엔티티(Clients, ProfanityWord, Report, Records) + Repository 포트

profanity-storage:rdb (Data Access - RDB)
  └─ domain 의 Repository 포트를 Spring Data JPA(Jpa*Repository)로 구현

profanity-storage:redis (Data Access - Cache)
  └─ RedisConfig / RedisTemplate / properties

profanity-shared (Common)
  └─ ApiResponse / status code / elapsed / WebConfig / Mode 등 공통 응답·상수
```

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
- 동기 필터링 후 `FilterEvent` 발행 → `records` 저장 (trackingId, mode, apiKey, 요청문, 검출 단어, referrer, ip)

### 인증 체계 (`profanity-api/.../security`)
- `X-API-KEY` 헤더 기반 Stateless Spring Security
- `CustomAuthenticationFilter`(OncePerRequestFilter) → `AuthenticationService`가 메타데이터 조회 후 권한을 `ROLE_` 접두사로 변환 → `SecurityContext`
- `ExcludePath` enum으로 필터 제외 경로 관리 (actuator, clients/register, send-email, health, ping, resource)
- `@VerifiedClientOnly` + `ClientVerificationAspect`(`@Around @Order(1)`): BLOCK/DISCARD 권한 클라이언트를 403으로 차단
- 권한(`PermissionsType`): READ / WRITE / DELETE / BLOCK / DISCARD (기본 [READ])

### 스케줄러 (`profanity-domain/.../application/manage`)
- `SyncScheduler`: `@Scheduled(fixedDelay = 60000)` — 1분마다 DB 단어 수 비교, 변경 시에만 Trie 재동기화. (`@SchedulerLock`은 주석 처리되어 미적용)
- `DailyReportScheduler`: `@Scheduled(cron = "0 0 1 * * ?")` — 매일 01:00. ShedLock 5.10.0 `@SchedulerLock` 적용됨

## REST 엔드포인트 (`profanity-api/.../presentation`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/filter` (JSON) | 동기/비동기 필터링, `@Cacheable` |
| POST | `/api/v1/filter` (form-urlencoded) | 동기 필터링 |
| POST | `/api/v1/filter/advanced` | 단일 word 마스킹 |
| GET | `/api/v1/clients` | 클라이언트 정보 조회 |
| DELETE | `/api/v1/clients` | 클라이언트 폐기 |
| POST | `/api/v1/clients/register` | 신규 등록 (인증 불필요) |
| POST | `/api/v1/clients/update` | 정보 수정 |
| POST | `/api/v1/clients/reissue` | API Key 재발급 |
| GET\|PUT | `/api/v1/clients/send-email` | 이메일 인증 코드 발송 / 검증 |
| POST | `/api/v1/word/request` | 단어 추가/제거/수정 요청 |
| POST | `/api/v1/word/accept/{requestId}` | 단어 요청 승인 (WRITE 권한) |
| GET | `/api/v1/sync?password=...` | 수동 동기화 (관리자) |
| GET | `/api/v1/health`, `/api/v1/ping` | 헬스 체크 |

- 응답은 HTTP 200 고정, 비즈니스 결과는 `status.code`(KISO 호환: 2000/2020/4000/4010/4030/4290/5000)로 전달

## 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 로컬 실행
./gradlew :profanity-api:bootRun

# 테스트 (restdocs 태그 제외, 루트 test가 전 모듈 test에 의존)
./gradlew test

# RestDocs 생성 (restdocs → asciidoctor → copyRestDocs)
./gradlew :profanity-api:generateRestDocs
```

## 테스트 컨벤션

- JUnit 5(Platform), 한글 `@DisplayName` + `@Nested` BDD 스타일
- API 계층: `@WebMvcTest` + `MockMvc`. RestDocs 테스트는 `@Tag("restdocs")`로 분리되어 기본 `test`에서 제외
- 도메인 계층: Mock보다 **테스트 더블 우선** — `Inmemory*Repository`, `FakeApplicationEventPublisher` 등 실제 구현 사용
- Mockito는 외부 의존 격리가 필요한 일부(EmailService 등)에만 제한적으로 사용
- 현재 `*Test.java` 14개

## 배포 (K3s + ArgoCD GitOps)

- **GitOps 경로**: `deploy/application.yaml`(ArgoCD Application) → `deploy/overlays/production`(Kustomize, namespace `profanity-production`, automated prune/selfHeal)
- **이미지**: `docker-registry.bottle-note.com/profanity-api`(Zot 레지스트리). ArgoCD Image Updater가 `v{semver}` 태그 감지 시 kustomization `newTag` 자동 갱신·커밋
- **Deployment**: replicas 2, `arm64` nodeSelector, RollingUpdate, port 8080, `SPRING_PROFILES_ACTIVE=prod`, 시크릿은 ExternalSecret(1Password Connect)로 주입된 `profanity-secrets`
- **라우팅**: Gateway API `HTTPRoute` (`main-gateway`, envoy-gateway-system) → `api.profanity.kr-filter.com` → `profanity-api:80`
- **Redis(운영)**: `redis:7-alpine` 단일 Deployment, 영속성 없음(`--save ""`), `allkeys-lru` 캐시 전용
- **Dockerfile**: `eclipse-temurin:21` 멀티스테이지, `./gradlew build -x test` 후 `profanity-api` JAR 실행
- **인프라 정의**: `module.platform` 서브모듈(ArgoCD, cert-manager, Envoy Gateway, external-secrets, image-updater, Zot). SOPS 암호화 시크릿 포함 — **값 열람·수정 금지**
- `script/`의 blue-green / docker-compose 스크립트는 레거시(현재 GitOps로 대체됨)
- CI: `.github/workflows/test.yml`(PR·main push 시 `./gradlew test`), `github-pages.yml`(RestDocs→Pages). `release.yaml`은 현재 빌드/배포 단계가 플레이스홀더 상태

## 코드 품질 및 주의사항

작업 전 반드시 인지해야 할 현 상태(추측 아님, 코드 확인 기반):

1. **version catalog 불일치**: `gradle/libs.versions.toml`에 Guava `33.0.0-jre`/JUnit `5.10.2`가 선언돼 있으나 실제 `build.gradle`은 Guava `31.1-jre`를 직접 선언 — catalog는 사실상 미사용. 의존성 변경 시 양쪽 불일치 주의
2. **Spotless / NullAway 미선언**: `.pre-commit-config.yaml`이 `gradlew spotlessApply`를 호출하지만 어떤 `build.gradle`에도 Spotless·NullAway 플러그인 선언이 없음. 글로벌 가이드의 "Spotless 적용" 전제는 이 레포에 해당하지 않음
3. **CORS**: `SecurityConfig`에서 `allowedOrigins(List.of("*"))` — 운영상 검토 필요
4. **`SyncScheduler` ShedLock 미적용**: 다중 인스턴스(replicas 2) 환경에서 중복 동기화 가능 (코드에 주석으로 인지됨)
5. **`NormalProfanityFilter.collect`**: `HashSet`이며 동기화 메서드 내 원자적 재할당으로만 수정됨
6. `@Cacheable` 캐시(Caffeine `request_filter`)에 명시적 TTL/무효화 전략 없음
7. 시크릿 파일(`.env`, `.secrets`, `*.enc.yaml`, `*.sops.yaml`, `module.secrets/`)의 값은 읽거나 출력하지 말 것
