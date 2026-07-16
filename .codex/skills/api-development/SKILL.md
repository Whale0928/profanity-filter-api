---
name: api-development
description: Implement or modify HTTP API endpoints in this profanity-filter-api repository. Use this skill whenever a request adds a controller endpoint, changes an API contract, adds OpenAPI documentation, decides whether an endpoint should appear in /openapi.json, or introduces dashboard, administrator, system, health, or duplicate transport APIs. Classify visibility before coding and keep controllers, composed OpenAPI annotations, public docs, architecture tests, and E2E tests aligned.
---

# API Development

## Core Policy

Treat `/openapi.json` as the allowlist of APIs intended for external integrators, not as an inventory of every implemented endpoint.

Classify the endpoint before implementation:

- External integrator API: document it with one detailed composed annotation from `app.openapi`.
- Dashboard, administrator, or system API: put `@Hidden` directly on the controller class or endpoint method.
- Duplicate transport representation of an already documented operation: use `@Hidden` and keep one canonical public operation.
- Ambiguous audience: stop and ask whether external integrators must implement against it.

Do not infer visibility from authentication alone. An authenticated API can still be public, while a technically public login callback can still be dashboard-internal.

## Implementation Workflow

1. Inspect the matching controller, security rule, DTOs, response types, existing `app.openapi` holder, README API list, and `OpenApiSpecE2ETest`.
2. State the audience classification and its code evidence before editing.
3. Preserve runtime mappings, security, and behavior unless the user explicitly asks to change them.
4. Apply exactly one documentation decision:
   - Public: add one runtime-retained composed method annotation under the matching `FooOpenApi` holder.
   - Hidden: add direct `@Hidden`; do not create or retain a composed operation annotation for that endpoint.
5. For a public operation, document the complete external contract:
   - summary and integration-focused description
   - request body, path/query/header parameters, media types, and validation constraints
   - concrete success and error response schemas
   - realistic but obviously fake examples
   - `ApiKeyAuth` when the endpoint requires an external API Key
6. Remove unused holder annotations and imports when an existing endpoint becomes hidden. Delete an empty holder class.
7. Align public surfaces. Remove hidden dashboard, administrator, and system paths from README and the Markdown assembled into `/overview.md`. Keep internal ADRs unless the user asks to delete them.
8. Update architecture and E2E coverage in the same change.

## Architecture Contract

Every non-document controller endpoint must satisfy one exclusive state:

- one `app.openapi` composed annotation and no effective `@Hidden`, or
- effective `@Hidden` on the method or owning controller and no `app.openapi` method annotation.

Allow direct Swagger dependency in controllers only for `@Hidden`. Keep all other Swagger operation details in `app.openapi` holders. Apply holder-name matching and `ApiKeyAuth` checks only to documented endpoints.

## Verification

Use MockMvc-based checks first so another worktree using port 8080 cannot interfere.

```bash
./gradlew --no-daemon staticCheck
./gradlew --no-daemon :profanity-api:unitTest --tests app.architecture.OpenApiArchitectureTest
./gradlew --no-daemon :profanity-api:e2eTest --tests app.e2e.OpenApiSpecE2ETest
```

The OpenAPI E2E test must assert both sides of the boundary:

- required public operation and security/schema details exist
- every newly hidden path is absent
- the total public operation count changes only when intentionally approved
- `/overview.md` exactly matches its classpath source fragments

If an actual server smoke test is necessary, avoid port 8080:

```bash
SERVER_PORT=18081 ./gradlew :profanity-api:bootRun
```

Then check `/openapi.json`, `/overview.md`, `/api/v1/health`, and `/api/v1/ping` on port 18081. Stop the process after verification.

## Current Visibility Examples

- Public: JSON filter, advanced filter, client/API Key APIs, word change request, health, ping.
- Hidden: dashboard authentication/session APIs, manual sync, word request approval, duplicate form-urlencoded filter operation.

These examples describe the current repository, but the audience rule is authoritative for future endpoints.
