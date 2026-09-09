# Autonomous functional and technical recette of Nudger

Run a resumable black-box campaign against the local Nuxt and front-api source while their read
paths use production data. Do not modify product source, production data or remote configuration.

## Parameters and resume

| Parameter | Values | Default |
|---|---|---|
| `MODE` | `desktop`, `mobile` or `all` | `all` |
| `LOCALES` | comma-separated supported locales | `fr,en` |
| `RUN_ID` | existing or new run id | current UTC timestamp plus short HEAD |

Start with `scripts/recette/board.py resume --run "$RUN_ID"`. If no campaign exists, initialize it
from `ops/recette/cases.yml`. A campaign is never reset; resume its first non-terminal case.

Record current HEAD and pre-existing dirtiness. Source remains read-only for the campaign. Runtime
evidence belongs under `artifacts/recette/<run-id>/`, which is ignored by Git.

## Stack

Run `scripts/recette/nudger-stack.sh start --run "$RUN_ID"`. The launcher starts:

- front-api locally with `SPRING_PROFILES_ACTIVE=devsec,local`;
- Nuxt locally in SSR mode, pointing its BFF to that front-api;
- separate log files for front-api and Nuxt SSR.

The default recette does not start api or Elasticsearch because their background jobs may write.
Wait for the exact `STACK READY:` line, then run `status`. A missing ignored devsec configuration is
a bootstrap blocker; never search other files or print a candidate credential.

Production access is controlled by agent discipline, as selected by the owner. Remote requests are
limited to `GET`, `HEAD` and `OPTIONS`. Never exercise login, contribution, refresh, generation,
index, assignment, cache invalidation or admin mutation against a production URL. Local browser and
BFF operations may use other methods only when their entire effect remains local.

## MCP and browser posture

Run `scripts/mcp/doctor.sh --core` before the campaign. Use the real Playwright MCP browser, the
Vuetify MCP when assessing component behavior and Nuxt MCP when available. Use Docker MCP only for
read-only status and logs. Do not substitute source inspection for an observable browser verdict.

Viewports are desktop `1440x900` and mobile `390x844`. Clear storage and service workers for the
local origin on a new campaign. Verify `window.innerWidth`, locale and actual route before evidence.

## Case protocol

Before each case:

1. Claim it with `board.py claim` and capture byte offsets with `nudger-stack.sh offsets`.
2. Arm browser console, page-error and failed-response capture.
3. Perform the user intent and wait for network and rendering to settle.
4. Run `scripts/recette/log-audit.py` with the saved offsets.
5. Attach sanitized screenshots/traces and immediately record the verdict.

Statuses are `PASSED`, `FAILED`, `BLOCKED` and `NOT_APPLICABLE`. A failure names expected versus
observed behavior and has reproducible evidence. Try at most three meaningfully different attempts.

Inspect every new log segment for:

- i18n missing keys, fallback failures, untranslated technical keys and locale mismatch;
- Java `ERROR`, stack traces, uncaught exceptions, Problem Details and HTTP 5xx;
- Nitro or Vue SSR failures, hydration mismatch, server-side DOM access and unhandled promises;
- browser console errors, failed resources, redirect loops and unexpected remote mutations.

Evaluate nominal and empty states, invalid routes/parameters, loading and recovery, keyboard and
screen-reader cues, responsive overflow, FR/EN parity, metadata/canonical/hreflang, and clean SSR.
Sample product and category routes from public sitemaps rather than a dated hard-coded catalogue.

## Failure and close

A pre-readiness failure is a bootstrap failure: preserve launcher evidence and stop without adding
false product cases. After readiness, continue independent cases after ordinary failures. Stop only
if the stack cannot restart, the browser is unavailable after three approaches, or three
consecutive cases share the same blocker.

At the end run `board.py matrix` and `board.py report`. Every case must be terminal or explain its
blocker. Stop the stack but keep the campaign artifacts for resume. Report the revision, matrix,
highest-priority failures, recurring i18n/server/SSR signals and untested boundaries. Do not edit
source, create issues, commit, push or deploy from this prompt.
