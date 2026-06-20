# 4. 공통 테스트 지원을 위한 test support 모듈 구성

## Status
제안 (2026.06.20)

## Context
현재 테스트 지원 코드는 각 모듈의 `src/test/java` 아래에 흩어져 있다. API 테스트는 `FakeProfanityHandler`, `FakeClientMetadataReader`, `SecurityFakeStubConfig` 같은 fixture를 사용하고, 도메인 테스트는 `InmemoryClientsRepository`, `InmemoryProfanityRepository`, `ClientTestFixture` 같은 테스트 더블을 직접 가진다.

이 구조에는 다음 문제가 있다.

- 도메인 fixture와 fake repository를 다른 모듈 테스트에서 재사용하기 어렵다.
- E2E 테스트에서 컨테이너, DB seed, 공통 테스트 유틸을 둘 위치가 명확하지 않다.
- 테스트 지원 코드의 소유 경계가 불명확해지면 API 모듈이 도메인 테스트 데이터나 인프라 준비 코드를 중복 정의할 가능성이 높다.

## Decision
공통 테스트 지원 경계로 `profanity-test-support` 모듈을 둔다.

- E2E 테스트에서 재사용할 컨테이너, DB seed, 테스트 유틸은 `profanity-test-support`에 둔다.
- 다른 모듈은 필요한 경우 테스트 의존성으로 `profanity-test-support`를 참조한다.
- 도메인 엔티티 fixture, fake repository, fixed key generator처럼 도메인 테스트 데이터와 직접 관련된 코드는 `profanity-test-support`로 이동할 수 있다.
- API 전용 fixture와 security stub은 `profanity-api` 테스트 영역에 남긴다.

## Consequences
- 공통 테스트 인프라와 fixture를 한 모듈에서 관리해 중복 작성을 줄일 수 있다.
- 운영 코드 모듈에 테스트 인프라 의존성이 섞이는 것을 피할 수 있다.
- 테스트 지원 모듈이 커지면 공개 테스트 API처럼 관리해야 하므로, fixture 이름과 패키지 구조를 도메인별로 유지해야 한다.
- `profanity-test-support`는 테스트 전용 모듈이므로 운영 코드에서 참조하지 않는다.

## Alternatives
- `profanity-domain`의 `java-test-fixtures`: 도메인 fixture만 공유하기에는 좋지만, 컨테이너와 DB seed 같은 E2E 지원 경계로는 좁아 제외한다.
- 각 모듈별 fixture 유지: 현재 구조를 유지할 수 있지만 중복과 일관성 문제가 남아 제외한다.
- API 모듈에 모든 fixture 집중: 도메인 테스트 지원 코드의 소유권이 API로 넘어가 의존 방향이 흐려지므로 제외한다.
