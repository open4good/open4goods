# Service secrets, beta and prod (render + restart)

Context: decision D140 (2026-10-22), ADR-0006, GOU-932 / GOU-933, GOU-957,
GOU-1014, GOU-1041 (credentials campaign GOU-1038).
Service secrets move from `open4goods-config` to the `beta` and `prod` GitHub
Environments of `open4good/open4goods` as they are rotated. A rotated secret is
rendered onto the host, and its services restarted, by the dispatchable
`serviceSecrets.yml`, without a deploy. The deploy workflows render the same
files with the same definition.

This document never lists secret values, key ids or fingerprints.

## Single definition

`scripts/deploy/render-service-secrets.sh [--if-present] [--source] <check|apply> <env> <service>`
(`env`: `beta` or `prod`; `service`: `api`, `ui`, `front-api`, `admin`,
`b2b-api`, `frontend` or `infra`) holds one table, service -> GitHub secret ->
target, documented at the top of the script. Every workflow that renders
secrets calls it:

| Workflow                 | Environment | Services                                   | Steps                                                                 |
| ------------------------ | ----------- | ------------------------------------------ | --------------------------------------------------------------------- |
| `serviceSecrets.yml`     | input       | input (`all` or one)                       | check, host check, then with `mode=apply`: render and restart         |
| `testAndPublishBeta.yml` | `beta`      | admin, api, front-api, ui, b2b-api         | `--if-present apply`, before "Publish Applications"                   |
| `frontend-ci.yml`        | `beta`      | frontend                                   | `--if-present apply`, before "Publish frontend"                       |
| `releaseDeployProd.yml`  | `prod`      | all (check); Spring services, frontend     | `--if-present check` in every mode; `apply` in `mode=release` only    |
| `infraSecrets.yml`       | input       | the target's consumers                     | `--source apply`, between the change at the source and the restart    |

Rows whose need is `source` hold a password that must first be changed at its
source (Elasticsearch) or a key of the compose infrastructure: only
`--source` renders them, and it renders nothing else. Deploys and
`serviceSecrets.yml` never do, so a value written in the Environment before
the source accepts it cannot break a service at the next deploy. Procedure:
[Infrastructure secrets](infra-secrets-rotation.md).

The release runs the script of the commit the workflow runs from (sparse
checkout into `deploy-tools/`), whatever ref it deploys: the table always
matches the secrets the workflow maps, and the rendered files only depend on
Spring Boot, not on the JAR.

### Kinds of target

- **Config tree** (`tree`, `tree-sa`): one file per Spring property,
  `/etc/open4goods/secrets/<host service>/<property>`, holding the exact value
  (no trailing newline), mode 600, directory 700 when new. Any value works:
  JSON, quotes, spaces, newlines. Every apply of a Spring service writes
  `SPRING_CONFIG_IMPORT=optional:configtree:/etc/open4goods/secrets/<host service>/`
  in `/etc/open4goods/<host service>.env`, which `publish-jars.sh` sources. The
  import is optional: an empty or missing tree is a no-op. It is written even
  when no tree secret is set, because `publish-jars.sh` sources every
  service's env file in the same shell during a rolling restart: a service
  without its own line would inherit the import of the service started before
  it. `tree-sa` values must also be a Google service account JSON key.
- **Env line** (`env`, `digits`): `KEY=<value>` in the service's env file.
  The value must match `[A-Za-z0-9/._-]` (digits only for `digits`), because
  the file is sourced by `publish-jars.sh` or read as a docker compose
  `env_file`. Used for the existing `api.env` lines and for the frontend.
- **File and path line** (`path-sa`): `API_GOOGLE_API_JSON` is written to
  `/etc/open4goods/secrets/google-indexation.json`, and
  `GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH` points `api` at it (GOU-933).

Host services and files: `api`, `ui`, `front-api` and `b2b-api` keep their
name; `admin` runs as `sbadmin` (`/etc/open4goods/sbadmin.env`,
`/etc/open4goods/secrets/sbadmin/`); the frontend reads
`/opt/open4goods/config/<env>/frontend/.env`, the `env_file` of
`docker-compose.frontend.yml`, which every `open4goods-config` deploy rewrites:
it is rendered before every publish.

### Precedence

`publish-jars.sh` starts the Spring services with
`spring.config.location=classpath:/application.yml,file:<config>/application-active.yml`
(sbadmin: the YAML file only). An import declared by `SPRING_CONFIG_IMPORT`
takes precedence over these locations, so the config tree beats
`application-active.yml` and `application.yml`, including its
`${ENV_VAR:default}` placeholders. The OS environment and `-D` system
properties still beat the tree: a property rendered in the tree must not also
be set as an environment variable in a file `publish-jars.sh` sources
(`config/<env>/infra/.env`, `/etc/open4goods/<service>.env`,
`config/<env>/<service>/.env`).

`RenderedSecretsConfigTreeTest`, in `api`, `ui`, `front-api`, `admin` and
`b2b-api`, starts Spring Boot with that layout and reads every rendered
property back through the class that consumes it: the tree value wins, the
YAML value stays without the import or with a missing tree, and the test
fails when a `tree` row of the script has no consumer. The `api.env` lines are
covered by `GoogleSearchConsoleServiceBindingTest`,
`AffiliationCredentialsBindingTest` and `EffiliationApiKeyBindingTest`; the
frontend lines by `frontend/server/utils/rendered-secrets-env.spec.ts` (Nitro
`NUXT_*` overrides) and `auth.spec.ts` (`FRONT_SECURITY_JWT_SECRET`).

## Secrets, targets and services

All secrets are optional except `GOOGLE_SSO_CLIENT_ID` / `_SECRET` for
front-api and the frontend: a missing one is a `::warning::`, is not rendered
and does not stop the others; the service keeps its previous render or,
failing that, the `open4goods-config` value. A set but malformed secret fails
before anything is written.

| Environment secret                | Service(s): target                                                                                                                                                            | Consumer                                                       |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `API_GOOGLE_API_JSON`             | api: `google-indexation.json` + `GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH`                                                                                                      | Metriks `GoogleSearchConsoleService` (`nudger-indexation`)     |
| `API_METRIKS_AWIN_PUBLISHER_ID`   | api: `METRIKS_AFFILIATION_AWIN_PUBLISHER_ID` (digits)                                                                                                                         | Metriks affiliation report (GOU-957)                           |
| `API_METRIKS_AWIN_API_TOKEN`      | api: `METRIKS_AFFILIATION_AWIN_API_TOKEN`                                                                                                                                     | Metriks affiliation report                                     |
| `API_METRIKS_EFFILIATION_API_KEY` | api: `METRIKS_AFFILIATION_EFFILIATION_API_KEY`                                                                                                                                | Metriks affiliation report                                     |
| `API_EFFILIATION_API_KEY`         | api: `AFFILIATIONCONFIG_EFFILIATIONAPIKEY`                                                                                                                                    | Effiliation product feeds (GOU-1014)                           |
| `API_VERTEX_API_JSON`             | api: tree `gen-ai-config.google-api-json` (service account JSON)                                                                                                              | Vertex Gemini (`vertexnudgerclient`), `AiProvidersConfiguration` |
| `API_VERTEX_BATCH_JSON`           | api: tree `vertex.batch.credentials-json` (service account JSON)                                                                                                              | Vertex batch (`VertexBatchConfig`)                             |
| `API_GOOGLE_SEARCH_API_KEY`       | api: tree `googlesearch.api-key`                                                                                                                                              | Custom Search (`GoogleSearchConfig`)                           |
| `API_OPENAI_API_KEY`              | api: tree `spring.ai.openai.api-key`                                                                                                                                          | Spring AI OpenAI                                               |
| `API_AWIN_ACCESS_TOKEN`           | api: tree `affiliation-config.awin-access-token`                                                                                                                              | Awin feeds                                                     |
| `API_KWANKO_TOKEN`                | api: tree `affiliation-config.kwanko-token`                                                                                                                                   | Kwanko feeds, Metriks affiliation report (GOU-1379)            |
| `API_TRADETRACKER_API_KEY`        | api: tree `affiliation-config.tradetracker-api-key`                                                                                                                           | TradeTracker feeds                                             |
| `API_EPREL_API_KEY`               | api: tree `eprel.api-key`                                                                                                                                                     | EPREL (`EprelServiceProperties`)                               |
| `O4G_ICECAT_PASSWORD`             | api, front-api: tree `icecat-feature-config.password`                                                                                                                         | Icecat account                                                 |
| `O4G_SBA_PASSWORD`                | api, ui, front-api, b2b-api: tree `spring.boot.admin.client.password`; ui, front-api: tree `o4g.actuator.monitor.password`; admin: tree `spring.boot.admin.instance-auth.default-password`, `o4g.sba.registration.password` | Spring Boot Admin registration and actuator access |
| `O4G_INTERNAL_API_KEY`            | api: tree `admin-key`; front-api: tree `front.affiliation-partners.api-key`                                                                                                   | front-api -> api calls (affiliation partners)                  |
| `O4G_CAPTCHA_SECRET`              | ui: tree `captcha-secret`; front-api: tree `captcha.secret-key`                                                                                                               | hCaptcha                                                       |
| `O4G_MAIL_PASSWORD`               | front-api, admin: tree `spring.mail.password`                                                                                                                                 | Gmail application password                                     |
| `FRONT_API_JWT_SECRET`            | front-api: tree `front.security.jwt-secret`; frontend: `FRONT_SECURITY_JWT_SECRET`                                                                                            | JWT signature (front-api signs, frontend verifies admin tokens) |
| `O4G_MACHINE_TOKEN`               | front-api: tree `front.security.shared-token`; frontend: `NUXT_MACHINE_TOKEN`, `MACHINE_TOKEN`                                                                                | `X-Shared-Token` machine calls                                 |
| `FRONT_API_GITHUB_TOKEN`          | front-api: tree `feedback.github.access-token`                                                                                                                                | GitHub feedback issues                                         |
| `GOOGLE_SSO_CLIENT_ID` (required) | front-api: `FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID`; frontend: `NUXT_GOOGLE_OAUTH_CLIENT_ID`; admin (optional, paired): tree `spring.security.oauth2.client.registration.google.client-id` | Google SSO ([Google SSO OAuth clients](google-sso-oauth-clients.md)); admin login (GOU-1091) |
| `GOOGLE_SSO_CLIENT_SECRET` (required) | frontend: `NUXT_GOOGLE_OAUTH_CLIENT_SECRET`; admin (optional, paired): tree `spring.security.oauth2.client.registration.google.client-secret`                          | Google SSO; admin login                                        |
| `O4G_ELASTICSEARCH_PASSWORD` (source) | api, ui, front-api, b2b-api: tree `spring.elasticsearch.password`                                                                                                        | Elasticsearch user `elastic` (`infraSecrets.yml` only)         |
| `B2B_API_POSTGRES_PASSWORD` (source) | b2b-api: tree `spring.datasource.password`; infra: `B2B_POSTGRES_PASSWORD`                                                                                                 | Postgres user of `b2b_postgres` (`infraSecrets.yml` only)      |
| `INFRA_*` (source)                | infra: `/etc/open4goods/infra.env` (Kibana, Plausible and its Postgres and ClickHouse)                                                                          | compose infrastructure (`infraSecrets.yml` only)               |
| `INFRA_GOOGLE_CLIENT_ID`, `INFRA_GOOGLE_CLIENT_SECRET` (source) | infra: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` in `/etc/open4goods/infra.env` (beta)                                                         | Google OAuth client of Plausible's Search Console integration (`infraSecrets.yml` `target=plausible-google` only) |

`b2b-api` registers with Spring Boot Admin only when its configuration enables
the client (`B2B_SBA_CLIENT_ENABLED`); the tree then replaces its
`${B2B_SBA_PASSWORD:}` default.

Not rendered, on purpose:

- `API_ADMIN_KEY`: api accepts only `admin-key` (or a `test-keys` entry), and
  front-api sends `front.affiliation-partners.api-key` to api, so both must
  hold the same value: `admin-key` reads `O4G_INTERNAL_API_KEY`.
- `API_URLFETCHER_PROXY_PASSWORD`: no deployed service depends on the
  `urlfetching` module that reads `urlfetcher.proxy.*`.
- `feed.api-key` (api) and `front.review-generation.api-key` (front-api): no
  class reads them.
- ui: `icecat-feature-config.password`, `feedback.github.access-token` and
  `spring.mail.password` are present in its beta configuration, but ui ships
  neither the Icecat, feedback nor mail module.
- Infrastructure secrets: see
  [Infrastructure secrets](infra-secrets-rotation.md) (GOU-1092). Redis
  (`spring.data.redis.password`) and XWiki (`xwiki.password`) are not
  rendered: no deployed service reads them any more.

The organisation secrets `MACHINE_TOKEN` / `BETA_MACHINE_TOKEN` are read by no
workflow of this repository.

### Coupled secrets

These values must reach every consumer in the same run, otherwise the
services stop talking to each other until the next restart: run
`serviceSecrets.yml` with `service=all`, or a full deploy (beta: deploy the
Spring services and the frontend one after the other, front-api first).

| Secret                 | Consumers                                                                                       |
| ---------------------- | ----------------------------------------------------------------------------------------------- |
| `O4G_SBA_PASSWORD`     | SBA clients (api, ui, front-api, b2b-api) and admin's registration check; ui and front-api actuators and admin's instance auth |
| `O4G_MACHINE_TOKEN`    | front-api (checks `X-Shared-Token`) and the frontend (sends it); `publish-frontend.sh` refuses to publish when front-api rejects the frontend's token |
| `FRONT_API_JWT_SECRET` | front-api (signs) and the frontend (verifies admin access tokens)                               |
| `O4G_INTERNAL_API_KEY` | front-api (sends) and api (`admin-key`, its only accepted key)                                  |

`O4G_ELASTICSEARCH_PASSWORD` is coupled with its source: Elasticsearch
(`elastic`) and its four consumers (api, ui, front-api, b2b-api) change in one
run of `infraSecrets.yml` `target=elasticsearch`, never of this workflow.
Likewise `B2B_API_POSTGRES_PASSWORD` (`b2b_postgres` and b2b-api,
`target=b2b-postgres`).

`O4G_MACHINE_TOKEN` and `FRONT_API_JWT_SECRET` reach the frontend as env
lines, so their value must match `[A-Za-z0-9/._-]`.

### Paired secrets

admin receives `GOOGLE_SSO_CLIENT_ID` and `GOOGLE_SSO_CLIENT_SECRET` together
or not at all: one set without the other fails the script, even with
`--if-present`, before anything is written, so the admin never mixes the
site's client id with its former client's secret. Admin reuses the site's SSO
client (decision of 2026-10-23, GOU-1038); the client must list the admin's
redirect URIs before the first render (see
[Google SSO OAuth clients](google-sso-oauth-clients.md#redirect-uris-to-register-on-the-web-client)).
Access stays gated by `AdminOidcUserService` (verified e-mail in
`admin.security.google-sso.role-allowlist`), proven by `AdminGoogleSsoAccessTest`.

### How a file is written

`scripts/deploy/push-remote-file.sh <remote-file> <VAR>` writes a whole file
(`--exact`: without a trailing newline, for config tree files);
`scripts/deploy/push-remote-env.sh <remote-env-file> <KEY>...` replaces keys in
an env file and keeps its other lines. Both:

- read the value from the step environment, write it to a umask 077 temp file
  on the runner and stream it over SSH stdin: never on a command line, in an
  artifact or in a log;
- create the parent directory if needed (mode 700 when new), build the file
  next to its target and move it into place atomically, mode 600, keeping the
  owner of the previous file;
- accept only safe characters in the remote path (`push-remote-file.sh` only
  writes under `/etc/open4goods/`). `push-remote-env.sh` values are limited to
  `[A-Za-z0-9/._:-]` (`:` for the config tree import); the render script keeps
  secret values of env lines to `[A-Za-z0-9/._-]`.

`scripts/tests/push-remote-file.test.sh` tests the three scripts against a
fake `ssh`, and checks that every workflow step calling the render script maps
the secrets of the services it renders (run by the Lint Suite workflow).

## Workflow `serviceSecrets.yml`

Actions > "Service secrets (render + restart)" > Run workflow. It replaces
`prodSecrets.yml`.

Inputs:

- `env`: `beta` (default) or `prod`; the job declares `environment: <env>`
  and targets that environment's host;
- `service`: `all` (default), `api`, `ui`, `front-api`, `admin`, `b2b-api` or
  `frontend`;
- `mode`: `test` (default) or `apply`. `apply` only runs on `main`; `test`
  writes nothing and may run from a branch.

The job shares the concurrency group of the deploy it would race:
`deploy-prod` (release), `deploy-beta-backend` (`testAndPublishBeta.yml`, also
used for `service=all`) or `deploy-beta-frontend` (`frontend-ci.yml`), with
`cancel-in-progress: false`: it never cancels a deploy and waits for it; a
release, which cancels in progress, may still interrupt it. Each secret is mapped only when the
selected service uses it.

| Step                                                | `test` | `apply` |
| --------------------------------------------------- | ------ | ------- |
| Check the services' secrets (no SSH)                | yes    | yes     |
| Check the host (read only, over SSH)                | yes    | yes     |
| Render the services' secrets                        | no     | yes     |
| Restart (`publish-jars.sh`, `publish-frontend.sh`)  | no     | yes     |

- The secret check fails when a required secret (Google SSO) is missing or a
  set secret is malformed; each missing optional secret is a warning. The
  output lists the secrets that would be rendered and their target, never a
  value.
- The host check verifies, for each Spring service, that `/etc/open4goods`,
  `secrets/` and `secrets/<host service>/` (if present) are writable, that its
  env file is readable if present, that `publish-jars.sh` exists and that
  `/opt/open4goods/runtime/<host service>.jar` is identical to
  `/opt/open4goods/latest/<host service>-latest.jar`; for the frontend, that
  its `.env` is writable, that `publish-frontend.sh` exists and that the
  running bundle (`bin/frontend-ssr/.output`) has the same `nitro.json` and
  `server/index.mjs` as `latest/frontend-ssr/.output`. A restart deploys
  `latest`: when it differs, the workflow fails before any rendering.
- `service=all` restarts every Spring service (`publish-jars.sh <env>
  restart`: sbadmin, api, front-api, ui, b2b-api, each waiting for its health
  check) then publishes the frontend. A single service restarts alone; the
  summary then warns that coupled secrets need `service=all`.
- The `apply` summary shows the written paths with their mode and owner, the
  env keys replaced, the service account `client_email` and the first 8
  characters of `private_key_id`, and the secrets not set. No step uses
  `set -x`.

## Deploy behaviour

Without any new secret in the GitHub Environment, a deploy behaves as before:
the script writes the lines it already wrote (Google SSO, and in prod the
`api.env` lines), no tree file, and `SPRING_CONFIG_IMPORT` (an optional import
of a missing directory, a no-op); every other value stays the
`open4goods-config` one.

- `testAndPublishBeta.yml` (each merge on `main` touching the backend, or a
  dispatch, GOU-1036) renders the Spring services from `beta` before "Publish
  Applications", which restarts them; `frontend-ci.yml` renders the frontend
  before "Publish frontend". A missing secret is a warning.
- `releaseDeployProd.yml` checks every service in every mode, right after the
  checkout, and fails on a set but malformed secret before any build. With
  `mode=release`, it renders the Spring services before "Start Applications"
  and the frontend before "Publish frontend". The "Check Google SSO secrets"
  step still fails when an SSO secret is missing from `prod`.

## Rotation procedure

1. Access creates the new value and stores it in the `beta` or `prod`
   Environment of `open4good/open4goods`, under the name of the table. The
   value is never pasted in an issue, a comment or a log.
2. Ops runs `serviceSecrets.yml` with that `env`, the service (or `all` for a
   coupled secret) and `mode=test`: every check is logged, nothing is written
   or restarted.
3. Ops runs it with `mode=apply` (covered by the campaign's board approval).
4. Proof: the run summary (paths in mode 600, `client_email`, key id prefix,
   names of the lines written and of the secrets not set), the services'
   health checks, then the functional check of the credential listed in the
   campaign manifest (for `API_GOOGLE_API_JSON`: Metriks
   `seo.gsc.indexed_pages` > 0; for an affiliation secret:
   `GET /api/metriks/affiliation` shows the network's events in `ok`).
5. Access revokes the previous value, then the campaign removes the copy left
   in `open4goods-config`.

To roll back before revocation, put the previous value back in the
Environment secret and run `apply` again. A tree file or env line whose secret
is later deleted from the Environment stays on the host until it is removed
by hand.
