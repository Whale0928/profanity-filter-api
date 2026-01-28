# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Aho-Corasick 알고리즘 기반의 고성능 비속어 필터링 REST API 서비스.

- **기술 스택**: Java 21, Spring Boot 3.4, JPA, Redis, MySQL
- **아키텍처**: Clean Architecture 기반 멀티모듈 구조
- **배포**: K3S

## 멀티모듈 구조

```
profanity-api (Presentation)
  ├─ REST Controllers
  ├─ Security (API Key 인증)
  └─ Exception Handlers

profanity-domain (Business Logic)
  ├─ NormalProfanityFilter (Aho-Corasick Trie)
  ├─ ClientsCommandService
  ├─ SyncScheduler (비속어 DB 동기화)
  └─ Event Handlers

profanity-storage (Data Access)
  ├─ rdb: JPA Repositories
  └─ redis: Cache Configuration

profanity-shared (Common)
  ├─ DTOs & Response Models
  └─ Constants & Utilities
```

## 핵심 컴포넌트

### NormalProfanityFilter
- Aho-Corasick Trie 기반 다중 패턴 매칭 (O(n+m) 시간복잡도)
- `volatile Trie trie` - Lock-free 동기화
- 1분마다 DB에서 비속어 목록 동기화

### 인증 체계
- API Key 기반 인증 (x-api-key 헤더)
- CustomAuthenticationFilter → SecurityContext
- ClientVerificationAspect (@VerifiedClientOnly)

### 필터링 모드
- QUICK: 첫 번째 매칭만 반환
- NORMAL: 모든 매칭 반환
- FILTER: 마스킹 처리 (**)

### 비동기 처리
- CompletableFuture 기반
- 콜백 URL로 결과 전달

## 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 로컬 실행
./gradlew :profanity-api:bootRun

# 테스트
./gradlew test

# RestDocs 생성
./gradlew :profanity-api:generateRestDocs
```

## 배포

~~Blue-Green 배포 (Docker Compose)~~ → K3S로 이전 중

## 주요 이슈

1. `NormalProfanityFilter.collect` - Thread-unsafe (ConcurrentHashMap.newKeySet() 필요)
2. SecurityConfig CORS - `allowedOrigins("*")` 보안 취약
3. 캐시 무효화 전략 없음
4. SyncScheduler ShedLock 미적용
