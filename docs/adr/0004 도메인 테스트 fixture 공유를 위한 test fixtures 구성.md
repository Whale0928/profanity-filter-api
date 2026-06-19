# 4. 도메인 테스트 fixture 공유를 위한 test fixtures 구성

## Status
제안 (2026.06.20)

## Context
현재 테스트 지원 코드는 각 모듈의 `src/test/java` 아래에 흩어져 있다. API 테스트는 `FakeProfanityHandler`, `FakeClientMetadataReader`, `SecurityFakeStubConfig` 같은 fixture를 사용하고, 도메인 테스트는 `InmemoryClientsRepository`, `InmemoryProfanityRepository`, `ClientTestFixture` 같은 테스트 더블을 직접 가진다.

이 구조에는 다음 문제가 있다.

- 도메인 fixture와 fake repository를 다른 모듈 테스트에서 재사용하기 어렵다.
- 테스트 지원 코드의 소유 경계가 불명확해지면 API 모듈이 도메인 테스트 데이터를 중복 정의할 가능성이 높다.
- 별도 테스트 지원 모듈을 바로 만들면 아직 작지 않은 구조 비용이 생긴다.

## Decision
우선 `profanity-domain`에 Gradle `java-test-fixtures`를 적용해 도메인 소유 테스트 fixture를 공유한다.

- 도메인 엔티티 fixture, fake repository, fixed key generator처럼 도메인 테스트 데이터와 직접 관련된 코드를 `profanity-domain` test fixtures로 이동한다.
- 다른 모듈은 필요한 경우 `testImplementation(testFixtures(project(":profanity-domain")))` 형태로 도메인 fixture를 참조한다.
- API 전용 fixture와 security stub은 `profanity-api` 테스트 영역에 남긴다.
- 별도 `profanity-test-support` 모듈은 공통 컨테이너, E2E 베이스 클래스, 여러 모듈이 공유하는 Spring 테스트 DSL이 충분히 커질 때 다시 결정한다.

## Consequences
- 도메인 fixture의 소유권이 `profanity-domain`으로 정리되고, 중복 fixture 작성을 줄일 수 있다.
- 멀티모듈 의존 방향을 유지하면서 테스트 코드만 필요한 범위로 공유할 수 있다.
- 별도 테스트 지원 모듈을 만들지 않으므로 초기 변경 범위가 작다.
- test fixtures API가 커지면 테스트 지원 코드도 공개 계약처럼 관리해야 하므로, fixture 이름과 패키지 구조를 도메인별로 유지해야 한다.

## Alternatives
- 별도 `profanity-test-support` 모듈: 공유 범위가 아직 작고 컨테이너 기반 RDB 통합 테스트도 이번 범위에서 제외되어 보류한다.
- 각 모듈별 fixture 유지: 현재 구조를 유지할 수 있지만 중복과 일관성 문제가 남아 제외한다.
- API 모듈에 모든 fixture 집중: 도메인 테스트 지원 코드의 소유권이 API로 넘어가 의존 방향이 흐려지므로 제외한다.
