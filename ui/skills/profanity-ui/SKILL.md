---
name: profanity-ui
description: Use when modifying the profanity-filter-api UI under ui/. Follow project engineering policy, design system policy, routing contracts, hook boundaries, and verification rules before changing screens or menus.
---

# Profanity UI Skill

Use this skill for work under `ui/`.

## Required References

- Read `policies/engineering.md` before code refactoring, hooks, routing, API integration, or tests.
- Read `policies/design-system.md` before changing layout, colors, navigation, components, copy, or state presentation.

## Workflow

1. Check current git status and do not overwrite unrelated changes.
2. Capture or otherwise verify current behavior before risky UI refactors.
3. Add or update a failing test first for behavior changes.
4. Implement the smallest change that satisfies the policy and test.
5. Run `npm run typecheck`, relevant `npm run test:*` scripts, and `npm run build`.
6. Re-check the affected route in a browser after refactoring.

## Boundaries

- Keep policy and agent guidance under `ui/policies` and `ui/skills`.
- Keep app runtime code under `ui/app/src`.
- Keep source-of-truth navigation in `src/app/routes.tsx`.
- Do not add future menus or API flows until they are needed.
