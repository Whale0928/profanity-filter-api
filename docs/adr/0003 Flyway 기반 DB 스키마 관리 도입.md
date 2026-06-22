# 3. Flyway 기반 DB 스키마 관리 도입

## Status
제안 (2026.06.20)

## Context
현재 RDB 설정은 `spring.jpa.hibernate.ddl-auto=validate`로 되어 있어 애플리케이션 기동 시 엔티티와 실제 DB 스키마의 정합성만 검증한다. 그러나 레포지토리 안에는 Flyway, Liquibase, `schema.sql`, `data.sql` 같은 스키마 생성·변경 이력이 없다.

이 상태에서는 다음 문제가 있다.

- 신규 환경에서 DB 스키마를 재현하는 source of truth가 코드 저장소에 없다.
- 테스트 환경에서 JPA를 포함한 E2E를 기동하려면 별도 스키마 준비가 필요하다.
- 운영 DB는 이미 존재하므로 단순히 Hibernate `create` 또는 `create-drop`에 의존할 수 없다.

## Decision
DB 스키마 변경 이력은 Flyway SQL migration으로 관리한다.

- migration 파일은 Spring Boot 기본 위치인 `classpath:db/migration`에 둔다.
- Spring Boot 기동 시 Flyway가 먼저 migration을 적용하고, 이후 JPA `ddl-auto=validate`가 엔티티와 스키마 정합성을 검증하는 흐름을 기준으로 한다.
- 기존 운영 DB에는 별도 baseline 전략을 적용한다. 이미 존재하는 테이블을 Flyway가 처음부터 생성하려고 시도하지 않도록 운영 반영 절차를 분리한다.
- Testcontainers 기반 RDB 통합 테스트는 이번 결정 범위에 포함하지 않는다. E2E 테스트에서 DB가 필요하면 H2와 Flyway migration 또는 테스트 stub 범위로 시작한다.

## Consequences
- 스키마 변경 이력이 코드 저장소에 남아 신규 환경과 테스트 환경에서 재현 가능해진다.
- 애플리케이션 기동 시 migration과 JPA validate를 함께 사용해 스키마 누락·불일치를 더 빨리 발견할 수 있다.
- 운영 DB 최초 도입 시에는 현재 스키마와 migration 파일의 기준점을 맞추는 baseline 절차가 필요하다.
- MySQL 전용 SQL을 사용할 경우 H2 기반 테스트와 호환되지 않을 수 있으므로 테스트용 migration 전략을 별도로 검토해야 한다.

## Alternatives
- Hibernate `create-drop`: 테스트 환경에서는 간단하지만 운영 스키마 변경 이력을 관리하지 못해 제외한다.
- Liquibase: 다른 프로젝트에서 사용 경험은 있으나, 이 프로젝트는 SQL DDL 중심의 단순한 변경 이력 관리가 우선이라 Flyway를 먼저 선택한다.
- 수동 DB 관리: 현재 문제를 유지하므로 제외한다.
