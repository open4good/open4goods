---
title: "Nudger local runtime on a shared build host"
normative: false
audience: PROJECT_SCOPED
---

# Nudger local runtime on a shared build host

Opt-in profile for `scripts/local/open4goods.sh` on a host shared with other tenants (template: [`.env.buildhost.example`](../../.env.buildhost.example)).
Point-in-time capacity measurements live on the tracking Paperclip issue, since they go stale; this page is the standing contract.

## What it does

- Ports: app/infra ports come from a configurable loopback block
  (`O4G_SHARED_HOST_PORT_MIN..MAX`, `_INFRA_PORT_MIN..MAX`), not the
  historical fixed ones. Every cross-service reference (ES/Postgres/Redis/
  XWiki, CORS, UI/docs/geocode/b2b-frontend URLs) is templated through the
  same variables in `ops/local/config/*.yml.example` and each module's
  `application-local.yml`, checked by `test_resolved_configs.py`.
- `preflight` (`O4G_SHARED_HOST=1`), before `up`/`restart`/`data full`:
  Docker rootlessness; port uniqueness/range/availability; data/config/
  backup paths inside `O4G_SHARED_HOST_APPROVED_ROOTS`; secrets past
  `CHANGE_ME`; cgroup mem/CPU/pids bounded with real headroom, not just
  quota; free disk (`data full` uses a stricter floor).
- `java_home_21()` only returns a self-verified JDK, failing loudly instead of using an unverified `JAVA_HOME`.
- `up`/`data full` flock outside any worktree (`O4G_SHARED_HOST_LOCK_DIR`), past the synchronous import call's completion.

No beta/prod mutation, host provisioning, or full stack/import here;
read-only backup access stays an ungranted, operator-side prerequisite, and
the floors above are defaults, not a profiled full-load ceiling. Tested:
syntax check, `docker compose config`, the Python unit suites, and
`test_preflight_gate.sh` against this host's real Docker/cgroup state.
