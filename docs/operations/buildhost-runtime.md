---
title: "Nudger local runtime on the shared build host: isolation and capacity report"
normative: false
audience: PROJECT_SCOPED
---

# Nudger local runtime on the shared build host: isolation and capacity report

> Companion WorkOrder: `local-full-stack-runtime`. Companion template:
> [`.env.buildhost.example`](../../.env.buildhost.example).

Read-only qualification of `scripts/local/open4goods.sh` on the shared build
host, alongside beta environments this profile stays clear of. No full
import, no beta/prod mutation. Measured 2026-09-29.

## Isolation inventory

| Item | Value |
|---|---|
| uid/gid | `paperclip` (1001), sole group `paperclip` -- no `open4goods` group membership |
| cgroup (user-1001.slice) | mem 16 GiB, cpu 6 cores, pids 4096 -- the session scope itself under-reports these as unlimited |
| Docker | rootless at `unix:///run/user/1001/docker.sock`, engine 29.1.3, root `~/.local/share/docker` (432 MiB); the system daemon is not reachable from this account |
| Java | `pom.xml` pins 21; `java` on PATH resolves 21.0.12.1, but `JAVA_HOME` defaults to a JDK 25 install, so `mvn spring-boot:run` used to fork under 25 |
| Maven / Node / pnpm | 3.8.7 / v24.21.0 / 12.6.0 |
| `rg` | absent for this account on this host -- `doctor()` and the contract test already depend on it |

## Storage

`/` has 115 GiB free (86% used); `/diskb` has 723 GiB free but every
`open4goods-*` directory there is root- or `open4goods`-group-owned, mode
750, with no ACL entry for `paperclip`. The persistent data root therefore
stays under `/`, sized against its 115 GiB, not `/diskb`'s.

Open prerequisite (host action, not taken here): read-only backup access
under `/diskb/open4goods-api-product-backup` and `/diskb/open4goods-backup`
needs an operator to run `setfacl -R -m u:paperclip:rX <dir>` or add
`paperclip` to group `open4goods`. Until then `PRODUCT_BACKUP_SOURCE_URI`
stays local-only and empty.

System-wide swap sat at 14/15 GiB occupied at measurement time; other
tenants on the same host are close to their own ceilings too.

## Port plan (nine apps, no collision)

The host already binds the runtime's historical ports to beta-adjacent
services (`3000, 3001, 5432, 5601, 6379, 8080-8092, 9200` all observed
LISTEN). The buildhost profile stays inside this agent's idle, QA-reachable
loopback block instead: api 4100, ui 4101, admin 4102, front-api 4103,
b2b-api 4104, exposed-docs 4105, geocode 4106, frontend 4107, b2b-frontend
4108 (4109 spare). Infra (not QA-browser-facing) sits adjacent: Elasticsearch
4150, Redis 4151, PostgreSQL 4152, Kibana 4153, XWiki 4154. All re-verified
idle with `ss -ltn` right before this report.

## Budget and preflight gate

`open4goods.sh preflight` (opt in: `O4G_SHARED_HOST=1`) checks, before `up`
runs anything: Docker security options include `rootless`; every app port
sits in `O4G_SHARED_HOST_PORT_MIN..MAX` (4100-4109 default); the data root's
filesystem carries at least `O4G_SHARED_HOST_MIN_FREE_GIB` free (20 default);
the effective cgroup ceiling (walking the ancestor chain, since the leaf
under-reports) covers `O4G_SHARED_HOST_MIN_MEM_GIB` / `_CPU` / `_PIDS` (8
GiB / 2 cores / 512 tasks default -- half this account's own measured slice,
leaving room for concurrent tenants). These floors are conservative
defaults, not a profiled ceiling for the full stack under load.

## Serialized full-run gate

`up` and `data full` now run under a non-blocking `flock` on
`.local/locks/full-run.lock`; a concurrent invocation exits with a "holds the
shared-host lock" message instead of competing silently for the same
budget. `restart`, `status`, `logs`, `down` stay untouched.

## Feasibility measured here

Short-test: `bash -n`, `docker compose config` (both env templates),
`python3 -m unittest test_product_backup_sample.py`, `open4goods.sh init` /
`doctor` / `preflight` against the buildhost port block (passes; correctly
refuses a port pushed outside 4100-4109), and the full-run lock rejecting a
concurrent holder. `doctor` still fails today on the missing `rg`, unrelated
to this change.

Full-volume feasibility is not asserted: running all nine services plus
infra under real load, or a full backup import, stays unverified pending the
backup-access prerequisite above and a dedicated profiling pass.

## Historical ledger

`.o4g/work/ledger/local-first-workorder-sequencing.yml` is unchanged.
