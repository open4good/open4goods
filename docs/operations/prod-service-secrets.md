# Production service secrets (render + restart)

Context: decision D140 (2026-10-22), ADR-0006, GOU-932 / GOU-933. Production
secrets move to the `prod` GitHub Environment of `open4good/open4goods` when
they are rotated. A rotated secret is rendered onto the prod host, and its
service restarted, by a `workflow_dispatch` workflow, without a release. The
release renders the same files with the same definition.

This document never lists secret values, key ids or fingerprints.

## Secrets, files and services

The single definition is `scripts/deploy/render-service-secrets.sh`, used by
both workflows:

| Environment secret    | File on the prod host                            | Line in `/etc/open4goods/<service>.env`                                          | Service | Consumer                                                                 | Rendered by                                               |
| --------------------- | ------------------------------------------------ | -------------------------------------------------------------------------------- | ------- | ------------------------------------------------------------------------ | --------------------------------------------------------- |
| `API_GOOGLE_API_JSON` | `/etc/open4goods/secrets/google-indexation.json` | `GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/etc/open4goods/secrets/google-indexation.json` (`api.env`) | `api`   | Metriks `GoogleSearchConsoleService` (`google-indexation.service-account-path`) | `prodSecrets.yml`, `releaseDeployProd.yml` (`mode=release`) |

- `API_GOOGLE_API_JSON` is the JSON key of the `nudger-indexation` Google
  service account. Before anything is written, the runner checks that it is a
  single JSON object with `type` `service_account` and non-empty
  `client_email` and `private_key`; neither the value nor the parser message is
  printed.
- `publish-jars.sh` (open4goods-config) sources `/etc/open4goods/<service>.env`
  before starting the service. `/etc/open4goods/` sits outside the tree that
  open4goods-config's deploy rsyncs with `--delete`, so the rendered files
  survive config deploys. The OS environment variable beats
  `application-active.yml`
  (`GoogleSearchConsoleServiceBindingTest` covers the relaxed binding of
  `GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH` to the `@Value`).
- In `GoogleSearchConsoleService`, `google-indexation.service-account-json`
  wins over `service-account-path` when both are set: the prod configuration
  must not set the JSON property.

### How a file is written

`scripts/deploy/push-remote-file.sh <remote-file> <VAR>` writes a whole file;
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
  `[A-Za-z0-9/._-]`.

`scripts/tests/push-remote-file.test.sh` tests the three scripts against a
fake `ssh` (run by the Lint Suite workflow).

## Workflow `prodSecrets.yml`

Actions > "Prod secrets (render + restart)" > Run workflow, on `main`.

Inputs:

- `service`: `api` (extend `render-service-secrets.sh` and the choice list to
  add a service);
- `mode`: `test` (default) or `apply`.

The job declares `environment: prod` and shares the `deploy-prod` concurrency
group with the release (`cancel-in-progress: false`: it never cancels a
release; a release started while it runs may cancel it).

| Step                                      | `test` | `apply` |
| ----------------------------------------- | ------ | ------- |
| Check the service's secrets (no SSH)      | yes    | yes     |
| Check the prod host (read only, over SSH) | yes    | yes     |
| Render the service's secrets              | no     | yes     |
| `publish-jars.sh prod restart <service>`  | no     | yes     |

- The secret check fails when a secret is missing or malformed.
- The host check verifies that `/etc/open4goods` (and `secrets/` if present)
  is writable, that `/etc/open4goods/<service>.env` is readable if present,
  that `publish-jars.sh` exists, and that
  `/opt/open4goods/runtime/<service>.jar` is identical (`cmp -s`) to
  `/opt/open4goods/latest/<service>-latest.jar`. `publish-jars.sh restart`
  copies `latest` to `runtime`: when they differ, the workflow fails before
  any rendering rather than deploy a JAR the last release did not start.
- `publish-jars.sh prod restart <service>` restarts that service only and
  waits for its health check (HTTP 401).
- The `apply` summary shows the written paths with their mode and owner, the
  service account `client_email` and the first 8 characters of
  `private_key_id`. No step uses `set -x`.

## Release behaviour

`releaseDeployProd.yml`:

- step "Check prod service secrets (api)", every mode, right after the
  checkout: fails when `API_GOOGLE_API_JSON` is set but malformed;
- step "Render api secrets on Prod", `mode=release` only, before
  "Start Applications" (which restarts every service).

Until `API_GOOGLE_API_JSON` exists in `prod`, both steps emit a `::warning::`
and render nothing: `api` keeps its current configuration (the
open4goods-config path). The same applies when the deployed ref predates
`render-service-secrets.sh`; files from a previous render are kept. The
dispatchable workflow, by contrast, fails when the secret is missing.

## Rotation procedure

1. Access creates the new key and stores it in the `prod` Environment of
   `open4good/open4goods` (`API_GOOGLE_API_JSON`). The key is never pasted in
   an issue, a comment or a log.
2. Ops runs `prodSecrets.yml` with `mode=test`: every check is logged, nothing
   is written or restarted.
3. Ops runs it with `mode=apply` (covered by the campaign's board approval).
4. Proof: the run summary (paths in mode 600, `client_email`, key id prefix),
   `api` answering 401 on `:8081`, then the functional check of the service
   (for `api`: Metriks `seo.gsc.indexed_pages` > 0, no authentication error in
   `nohup-api.log`).
5. Access revokes the previous key, then the campaign removes the copy left in
   open4goods-config.

To roll back before revocation, put the previous key back in the
Environment secret and run `apply` again.
