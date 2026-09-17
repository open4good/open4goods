---
title: "Configuration and deployment"
normative: false
audience: PROJECT_SCOPED
---

# Configuration and deployment

## Purpose

One entry point answering "how do I run this locally, and how does a real environment get its
configuration" -- both were previously scattered (README, `application.yml` defaults, a private
repository nobody outside the owner can open) or simply undocumented (the actual beta/prod
deployment mechanism, reverse-engineered from the hosts for this document). Read
[canonical decision 9](../00-canonical-decisions.md) before trusting a claim here that goes
unverified against the actual system: this document describes the state found on the real hosts on
2026-09-09, not an aspiration.

## Local development

Start with [the strict-local campaign](beta-development-campaign.md): Java 21, Maven, Node/pnpm
and Docker Compose. `scripts/local/open4goods.sh` owns startup and connects every native application
to loopback Elasticsearch, Redis and PostgreSQL. Module-specific local detail:

- `b2b-api` + `b2b-frontend`: [product-data-api-local-runbook.md](product-data-api-local-runbook.md).
- all services: `.env.local` and `.local/config/<service>.yml`, initialized from tracked templates
  and ignored by Git.

All packaged defaults hold no secrets. DEVELOPMENT loads only the `local` profile; `devsec` is not
part of the launcher and local runbooks do not use remote Elasticsearch credentials.

## Where configuration lives today

Three tiers, by how sensitive and how environment-specific a value is:

1. **Packaged public defaults** -- each module's own `application.yml` / `application-*.yml`,
   tracked in this repository. Safe as committed.
2. **The private `open4good/open4goods-config` repository** -- per-environment topology and every
   credential: `configs/{beta,prod}/{admin,api,b2b-api,front-api,ui}/application-active.yml`,
   `configs/{beta,prod}/{frontend,b2b-frontend}/.env`, `docker-compose/{beta,prod}/*` (including the
   Elasticsearch/Kibana files and certs -- see below). This repository is not part of this checkout
   and this document has no visibility into its contents; the full classified inventory of every
   tracked file and key it holds is [legacy-config-inventory.md](legacy-config-inventory.md).
3. **Local `.env.local` / `.local/config/*.yml` created by `open4goods.sh init`** -- gitignored,
   per the Local development section above.

Tier 2 is being migrated to GitHub Environments (public non-secret variables + environment
secrets) per [ADR-0006](../adr/0006-github-environments-and-systemd-runtime.md). The `beta` and
`prod` Environments exist; beta holds its target fingerprint, while both lack secrets and protection
rules. Until their inputs are populated, the private repository remains the actual source of truth
for beta and prod.

## How configuration reaches beta and prod today

A self-hosted GitHub Actions runner lives on each target host itself, checking out
`open4good/open4goods-config` privately. Three of its workflows do the real work, each dispatched
manually (or, for the first, on every push to its `main`):

| Workflow | What it does |
|---|---|
| `deployConfiguration.yml` | rsyncs (`easingthemes/ssh-deploy`) the private repo's `root/opt/open4goods/bin/` to `/opt/open4goods/bin/`, `docker-compose/{env}/` to `/opt/open4goods/latest/{env}/`, `datasources/` and `prompts/` to `/opt/open4goods/config/{datasources,prompts}/`, and **`configs/{env}/` to `/opt/open4goods/config/{env}/`** -- the step that lands every `application-active.yml` and `.env` on the host. |
| `publishInfra.yml` | SSHes in and runs `/opt/open4goods/bin/publish-infra.sh {env}`, which copies `docker-compose.infra.yml`, `kibana.yml`, `elasticsearch.yml`, `elastic-stack-ca.p12`, `elastic-certificates.p12`, `server.xml` and `xwiki.cfg` from `/opt/open4goods/latest/{env}/` into `/opt/open4goods/bin/` (the directory Docker Compose actually mounts from), then brings up `docker-compose.infra.yml` with `--env-file /opt/open4goods/config/{env}/infra/.env`. |
| `publishJars.yml` | SSHes in and runs `/opt/open4goods/bin/publish-jars.sh {env} [start\|stop\|restart] [service]`, which starts each Spring Boot jar with `-Dspring.config.location=classpath:/application.yml,file:/opt/open4goods/config/{env}/{service}/application-active.yml -Dspring.profiles.active=nudger,{env}`. |

The public repository's `releaseDeployProd.yml` is manually dispatched and deploys the Nuxt
`frontend`/`b2b-frontend` bundles (`frontend-ssr-{blue,green}`, `b2b-frontend` containers in
`docker-compose.frontend.yml`, deployed by `deployConfiguration.yml`'s bin-sync step). Every
remote release checks a host marker and a cluster fingerprint from its GitHub Environment before
writing; missing or unequal markers fail the release.

Host path summary: `/opt/open4goods/config/{env}/**` (rendered secrets and topology),
`/opt/open4goods/latest/{env}/**` (compose files and infra assets, pre-copy), `/opt/open4goods/bin/`
(what's actually mounted/run), `/opt/open4goods/runtime/` and `/opt/open4goods/run/` (live jars and
PID files -- deliberately outside the synced `bin/` tree, so a config deploy leaves a running
process's own jar alone).

## Beta DiskB cache cutover

The writable application cache has one canonical path, `/opt/open4goods/.cached`. On beta it is
backed by `/diskb/open4goods-cache` through `opt-open4goods-.cached.mount`; service templates use
the path as a mountpoint precondition. This avoids a missing DiskB mount silently growing
the root filesystem. DiskB also holds Elasticsearch data, so retain at least 30 percent free there
and at least 20 percent free on root.

Start every rehearsal with `scripts/deploy/inspect-cache-volume.sh`; it emits only aggregate
capacity, root-level class and ownership-mode counts. Use the authenticated cleanup dry run for
age and reclaimability, rather than listing cache entries. Prepare the source only through
`scripts/deploy/prepare-cache-volume.sh`, then install the runtime units and start the cache mount
before restarting a cache writer. Confirm both the mountpoint and its backing device with `findmnt`.
Do not list, copy or log legacy cache filenames because upstream request query values can be
sensitive. Remote files with the former URL-derived names are intentionally rebuilt after the
SHA-256 cache-key release rather than migrated. Product-resource cleanup stays behind its
authenticated dry-run and review process; batch and recovery data remain in place until their owner
verifies recovery.

## Elasticsearch specifically

Elasticsearch's config and key material are **not host-generated today** -- ADR-0006 states that as
the target, but the current reality (verified 2026-09-09 against both hosts) is that
`docker-compose/{beta,prod}/elasticsearch.yml`, `kibana.yml`, `elastic-stack-ca.p12` and
`elastic-certificates.p12` are ordinary tracked files in the private `open4goods-config` repository,
committed once and reused. They reach the host exactly like any other tier-2 asset: `deployConfiguration.yml`
rsyncs them into `/opt/open4goods/latest/{env}/`, then `publish-infra.sh` copies them into
`/opt/open4goods/bin/` right before `docker compose -f docker-compose.infra.yml up -d` starts the
`elastic` and `kibana` containers, which mount that directory. No SSH copy happens by hand and no
step regenerates the certificates per host or per deploy -- the same keystore pair has been reused
across deploys. `kibana.yml` also embeds `elasticsearch.password` in plaintext, and each Spring
service's own `application-active.yml` embeds `spring.elasticsearch.username`/`password` the same
way -- these are exactly the credentials `legacy-config-inventory.md` classified as `secret` and
that a future GitHub Environment migration would turn into an environment secret rather than a
file on disk.

## Full variable and secret inventory

Do not re-derive this by hand: [legacy-config-inventory.md](legacy-config-inventory.md) classifies
every one of the 157 tracked files in the private repository (public / variable / secret /
host-generated / archive-only), with a key-level appendix for the ten `application-active.yml`
files and the `.env` files. Read it before adding, renaming or moving a configuration key anywhere
in tier 2.

## Known documentation gap

`b2b-frontend/.env.example` currently carries variable names and comments (`infera.fr`,
`agent-core`, `clouders`) from an unrelated template project, not from `b2b-frontend`'s own
`nuxt.config.ts`. The `NUXT_*` names happen to still map correctly (Nuxt's `runtimeConfig` env
auto-binding), but the surrounding narrative does not describe this project. Verify against
`b2b-frontend/nuxt.config.ts`'s `runtimeConfig` block, not the example file's prose, until someone
rewrites it.
