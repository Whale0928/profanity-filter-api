---
name: manage-env-secrets
description: Manage this profanity-filter-api project's environment variables and secret stores. Use when adding, removing, auditing, syncing, or troubleshooting env vars in local development .env files, the module.secrets submodule, 1Password items, ExternalSecret-backed k3s deployment, OAuth/SSO credentials, DB/Redis credentials, mail credentials, server keys, or any task involving secret-source/database-source/op:// references.
---

# Manage Env Secrets

## Operating Rule

Treat every env value as secret unless proven otherwise. Do not print, quote, summarize, paste, log, commit, or include raw values in final answers. Report only key names, counts, file paths, item names, and verification status.

If a command may print values, either do not run it or redirect stdout away from the conversation and verify with a separate key-name-only command. This matters because `op item edit` default output can reveal non-concealed field values.

## Source Map

Local development source:

- `module.secrets/project/profanity-filter/backend/.env`
- Use this when the user asks to prepare or adjust local/submodule env values.
- This is a secrets submodule, but values are still not safe to display.

k3s deployment source:

- Vault: `profanity-filter`
- 1Password item: `secret-source`
- 1Password item: `database-source`
- `deploy/overlays/production/external-secret.yaml` extracts both items into the `profanity-secrets` Kubernetes Secret.

Current item ownership:

- `database-source`: `DB_DRIVER`, `DB_PASSWORD`, `DB_URL`, `DB_USERNAME`, `REDIS_MAIN_HOST`, `REDIS_MAIN_PASSWORD`, `REDIS_MAIN_PORT`
- `secret-source`: application/general secrets such as `MAIL_USERNAME`, `MAIL_PASSWORD`, `SERVER_PORT`, `SERVER_KEYCODE`, `SERVER_KEYCODE_ALGORITHM`, `ACTUATOR_PATH`, OAuth/SSO fields

Do not duplicate DB/Redis fields into `secret-source` while `database-source` owns them.

## Safe Workflow

1. Inspect state without values.

Use key-name-only commands:

```bash
sed -n 's/^\([A-Za-z0-9_][A-Za-z0-9_]*\)=.*/\1/p' module.secrets/project/profanity-filter/backend/.env | sort
op item get secret-source --vault profanity-filter --format json | ruby -rjson -e 'item=JSON.parse(STDIN.read); (item["fields"] || []).map { |f| f["label"] }.compact.sort.each { |label| puts label }'
op item get database-source --vault profanity-filter --format json | ruby -rjson -e 'item=JSON.parse(STDIN.read); (item["fields"] || []).map { |f| f["label"] }.compact.sort.each { |label| puts label }'
```

2. Classify each requested key.

- DB/Redis -> `database-source`
- OAuth/SSO -> `secret-source`
- Mail/server/app control -> `secret-source`
- Unknown key -> inspect deployment/config references before choosing an item

3. Apply only the needed delta.

- Add missing keys.
- Update changed keys.
- Remove stale keys only when the user asked or the owning manifest/config no longer references them.
- Keep database and app items separated.

4. Verify without values.

After modification, query field labels only and report:

- target item
- added/updated/removed key names
- missing keys, if any
- no raw values

## 1Password Update Rules

Prefer JSON template or piped input over assignment statements. `op item edit secret-source KEY=value` is unsafe because process arguments can expose values.

When using `op item edit`, do not allow default output into the chat. Redirect stdout to `/dev/null`, then verify labels separately:

```bash
op item get secret-source --vault profanity-filter --format json \
  | ruby -rjson -e '...build updated JSON without printing values...' \
  | op item edit secret-source --vault profanity-filter >/dev/null
```

Then verify:

```bash
op item get secret-source --vault profanity-filter --format json \
  | ruby -rjson -e 'item=JSON.parse(STDIN.read); (item["fields"] || []).map { |f| f["label"] }.compact.sort.each { |label| puts label }'
```

Use `CONCEALED` type for keys containing `PASSWORD`, `SECRET`, `TOKEN`, `KEY`, or `SIGNING_KEY` unless an existing field type is intentionally different.

Never use `--reveal` unless the user explicitly asks and the task cannot be done otherwise. Even then, avoid printing the value.

## Local .env Rules

When editing `module.secrets/project/profanity-filter/backend/.env`:

- Preserve existing unrelated lines and ordering where reasonable.
- Add new keys as `KEY=value`.
- Do not use root project `.env` or `sso/sso.env` as durable sources.
- Do not stage or commit ignored local scratch files.
- If committing the submodule, commit inside `module.secrets` first, then update the parent repo submodule pointer only if the user asks.

## Reporting

Keep reports short and factual:

- "Added 13 OAuth/SSO fields to `secret-source`; missing=none."
- "Skipped DB/Redis fields because `database-source` already owns them."
- "Did not print or reveal secret values."

Do not include command output that contains values.
