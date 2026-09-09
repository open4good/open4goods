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

Start with the [README's "Run in dev mode" section](../../README.md#run-in-dev-mode): Java 21,
Maven, and either Docker Compose (Elasticsearch + Redis, auto-started by
`spring-boot-docker-compose`) or `front-api`'s in-memory `local` profile (no infra at all). Module-
specific local runbooks:

- `b2b-api` + `b2b-frontend`: [product-data-api-local-runbook.md](product-data-api-local-runbook.md).
- `frontend`: a `.env` file with `API_URL`, `TOKEN_COOKIE_NAME`, `REFRESH_COOKIE_NAME` (README has
  the full block); gitignored, kept out of every commit.
- `api`, `ui`, `admin`: no dedicated runbook yet -- run with `-Dspring.profiles.active=dev` per the
  README's "Launching" section. Its `com.open4goods.ui.Ui`/`com.open4goods.ui.Api` IDE class paths
  predate the `org.open4goods` package rename and no longer resolve; use each module's
  `*Application` class instead (see `AGENTS.md` section 2's package layout).

All packaged `application.yml` defaults (one per `@SpringBootApplication` module) hold **no
secrets** -- that was `config-contract-and-environments`'s AC3. A local `devsec` profile run reads
real (non-production-secret) Elasticsearch/Redis credentials from that same profile's resources.

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
3. **Local `.env` / `application-active.yml` you create yourself** -- gitignored, per the Local
   development section above.

Tier 2 is being migrated to GitHub Environments (public non-secret variables + environment
secrets) per [ADR-0006](../adr/0006-github-environments-and-systemd-runtime.md). That migration is
`config-contract-and-environments`, currently **BLOCKED**: no `beta` or `prod` GitHub Environment
exists yet in this repository (verified via `gh api repos/open4good/open4goods/environments`,
2026-09-09) -- creating them is repository administration, an owner action. Until then, tier 2's
private repository remains the actual source of truth for beta and prod.

## How configuration reaches beta and prod today

A self-hosted GitHub Actions runner lives on each target host itself, checking out
`open4good/open4goods-config` privately. Three of its workflows do the real work, each dispatched
manually (or, for the first, on every push to its `main`):

| Workflow | What it does |
|---|---|
| `deployConfiguration.yml` | rsyncs (`easingthemes/ssh-deploy`) the private repo's `root/opt/open4goods/bin/` to `/opt/open4goods/bin/`, `docker-compose/{env}/` to `/opt/open4goods/latest/{env}/`, `datasources/` and `prompts/` to `/opt/open4goods/config/{datasources,prompts}/`, and **`configs/{env}/` to `/opt/open4goods/config/{env}/`** -- the step that lands every `application-active.yml` and `.env` on the host. |
| `publishInfra.yml` | SSHes in and runs `/opt/open4goods/bin/publish-infra.sh {env}`, which copies `docker-compose.infra.yml`, `kibana.yml`, `elasticsearch.yml`, `elastic-stack-ca.p12`, `elastic-certificates.p12`, `server.xml` and `xwiki.cfg` from `/opt/open4goods/latest/{env}/` into `/opt/open4goods/bin/` (the directory Docker Compose actually mounts from), then brings up `docker-compose.infra.yml` with `--env-file /opt/open4goods/config/{env}/infra/.env`. |
| `publishJars.yml` | SSHes in and runs `/opt/open4goods/bin/publish-jars.sh {env} [start\|stop\|restart] [service]`, which starts each Spring Boot jar with `-Dspring.config.location=classpath:/application.yml,file:/opt/open4goods/config/{env}/{service}/application-active.yml -Dspring.profiles.active=nudger,{env}`. |

The public repository's own `releaseDeployProd.yml` builds and tags releases and deploys the Nuxt
`frontend`/`b2b-frontend` bundles (`frontend-ssr-{blue,green}`, `b2b-frontend` containers in
`docker-compose.frontend.yml`, deployed by `deployConfiguration.yml`'s bin-sync step) -- those
containers read `env_file: /opt/open4goods/config/{env}/{frontend,b2b-frontend}/.env`, the same
files `deployConfiguration.yml` places.

Host path summary: `/opt/open4goods/config/{env}/**` (rendered secrets and topology),
`/opt/open4goods/latest/{env}/**` (compose files and infra assets, pre-copy), `/opt/open4goods/bin/`
(what's actually mounted/run), `/opt/open4goods/runtime/` and `/opt/open4goods/run/` (live jars and
PID files -- deliberately outside the synced `bin/` tree, so a config deploy leaves a running
process's own jar alone).

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
