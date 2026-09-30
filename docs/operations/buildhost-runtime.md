---
title: "Nudger local runtime on a shared build host"
normative: false
audience: PROJECT_SCOPED
---

# Nudger local runtime on a shared build host

Opt-in profile of `scripts/local/open4goods.sh` (`O4G_SHARED_HOST=1`) for a host
shared with other tenants, so a local stack collides with nothing else running
there. It is the shared-host reading of
[canonical decision 14](../00-canonical-decisions.md) and
[ADR-0014](../adr/0014-local-first-development-and-staged-promotion.md): isolated
stores and applications, no beta or prod dependency.

Every knob and default is in [`.env.buildhost.example`](../../.env.buildhost.example);
the gate is the script's own `preflight`, run before `up`, `restart` and
`data full`, covering Docker rootlessness, loopback port blocks (templated through
`ops/local/config/*.yml.example` and each module's `application-local.yml`),
approved data/config/backup roots, unreplaced `CHANGE_ME` secrets, and cgroup and
free-disk headroom. The script is the authority on the current list, with
`test_preflight_gate.sh` and `test_resolved_configs.py` as its tests.

Out of scope: beta and prod mutation, host provisioning, and read-only backup
access, an operator-side prerequisite. Preflight floors are defaults, not a
profiled full-load ceiling, and point-in-time capacity measurements live on the
tracking Paperclip issue, since they go stale.
