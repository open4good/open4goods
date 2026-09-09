---
title: "Legacy config repository inventory"
normative: false
audience: PROJECT_SCOPED
---

# Legacy config repository inventory

## Purpose

`config-contract-and-environments` AC1 classifies every tracked asset in the
private `open4good/open4goods-config` repository so later lots know what
migrates to packaged public defaults (AC3), what becomes a GitHub Environment
variable or secret (AC2), and what is archived without migration. Read-only
audit performed 2026-09-09 against the owner's existing local checkout
(`git ls-files`, 157 tracked paths at `origin/main` HEAD `e9ebc878f`); no value
in this document is a live credential. That checkout also holds untracked
local files -- personal/administrative documents and provisioning artifacts --
outside `git ls-files` and outside this inventory; see Findings.

## Classes

- **public** -- safe as a packaged default or committed here as-is.
- **variable** -- differs beta/prod, not sensitive; a GitHub Environment variable.
- **secret** -- credential or key material; a GitHub Environment secret.
- **host-generated** -- produced on the host at provisioning time (certs, service
  units); not migrated as data, regenerated per ADR-0009's runtime.
- **archive-only** -- retained in the private archive only; excluded from any
  future public location.

## File-level inventory (157 tracked files, `git ls-files` ground truth)

| Path glob | Count | Class | Notes |
|---|---|---|---|
| `configs/{beta,prod}/{admin,api,b2b-api,front-api,ui}/application-active.yml` | 10 | mixed | key appendix below |
| `configs/{beta,prod}/{b2b-frontend,frontend}/.env` | 4 | mixed | key appendix below |
| `configs/{beta,prod}/infra/.env` | 2 | mixed | beta has 4 keys; prod's copy is 0 bytes -- a live gap, see Findings |
| `configs/prod/ui/google-api.json` | 1 | secret | GCP service-account key (private_key field); host-generated candidate |
| `datasources/*.yml` | 3 | public | source-matching rules and display metadata, no credentials |
| `doc/cloudflare-config.md`, `doc/DEPLOYMENT.md` | 2 | public | operational notes, candidate content for `docs/operations/` |
| `docker-compose/{beta,prod}/docker-compose.{frontend,infra}.yml` | 4 | variable | service topology, references env files above |
| `docker-compose/{beta,prod}/elastic-certificates.p12`, `elastic-stack-ca.p12` | 4 | host-generated | keystores; regenerate on host, never migrate as data |
| `docker-compose/{beta,prod}/elasticsearch.yml`, `kibana.yml` | 4 | mixed | `kibana.yml` embeds an `elasticsearch.password` per environment -- both are live secrets in plaintext (see Findings for beta's) |
| `docker-compose/prod/server.xml`, `init.sql`, `xwiki.cfg`, `xwiki.cfg_remote` | 4 | mixed | `server.xml`/`xwiki.cfg*` embed XWiki datasource credentials; `init.sql` is schema, public |
| `docker-compose/docker-compose-github-runner.yml`, `docker-compose/README.md` | 2 | public | |
| `docker-compose/beta/strapi-app/{yarn.lock,node_modules/.yarn-integrity}` | 2 | public | unrelated dependency lockfile artifacts, orphaned |
| `.github/workflows/*.yml` | 6 | public | reference secrets by name, hold none |
| `.github/ISSUE_TEMPLATE/weekly-ritual.md` | 1 | public | |
| `.pre-commit-config.yaml`, `.lighthouserc.json`, `renovate.json`, `.gitignore`, `requirements.txt` | 5 | public | |
| `monitoring/**` | 3 | public | URL and sitemap check lists, no credentials |
| `prompts/**` | 4 | public | AI prompt templates |
| `scripts/**` (excluding `metriks/`) | 7 | public | read tokens from env, embed none (verified) |
| `scripts/metriks/**` | 19 | public | same -- providers read `*_TOKEN`/`*_API_KEY` from env |
| `root/etc/nginx/**`, `root/etc/sysctl.conf` | 21 | host-generated | vhost/proxy config, domain-scoped, no embedded credential found |
| `root/opt/open4goods/bin/*.sh` | 9 | public | deploy/backup/log scripts; read secrets from the rendered env files above, embed none |
| `root/root/authorized_keys` | 1 | secret-adjacent | public keys, but an access-control list -- treat as sensitive |
| `root/root/box-{beta,prod}.conf` | 2 | host-generated | |
| `root/root/README-prepare-box.md`, `root/README.MD` | 2 | public | the former has one exception, see Findings |
| `root/root/open4goods-prepare/**` | 34 | public | provisioning tool; reads `GHR_TOKEN`/`CF_API_TOKEN` from env or interactive prompt, embeds none, **except** one literal-looking example value -- see Findings |

Column sums to 157.

## Key-level appendix: application-active.yml (beta and prod share key sets; deltas noted)

| Key pattern | Class |
|---|---|
| `server.port`, `server.forward-headers-strategy`, `logging.level.*`, `spring.application.name`, `spring.thymeleaf.cache`, `spring.docker.compose.enabled`, `*.thread-pool-size`, `*.max-*`, `*.min-*`, `*.page-size`, `*.queue-max-size`, `review.generation.preferred-domains*`, `review.generation.excluded-domains`, `feed.providers.*.default-csv-properties.*` (field-mapping rules), `web-config.*` (non-auth), `urlcheck.*`, `banchecker-config.ips`, `namings.*` | public |
| `xwiki.base-url`/`baseUrl`, `spring.boot.admin.client.url`, `spring.elasticsearch.uris`, `spring.data.redis.host`/`port`, `spring.mail.host`/`port`, `b2b.public-base-url`, `blog.static-domain`, `front.contact.email`, `vertex.batch.project-id`/`bucket`, `geocode.maxmind.url`, `email`, `root-folder` | variable |
| `xwiki.username`/`password`, `spring.boot.admin.instance-auth.default-user-name`/`default-password`, `spring.boot.admin.client.username`/`password`, `spring.elasticsearch.username`/`password`, `spring.data.redis.password`, `spring.datasource.username`/`password`, `spring.mail.username`/`password`, `spring.ai.openai.api-key`, `admin-key`, `eprel.api-key`, `icecat-feature-config.user`/`password`, `affiliation-config.*-token`/`*-api-key`, `googlesearch.api-key`, `amazon-config.access-key`/`secret-key`, `urlfetcher.proxy.username`/`password`, `vertex.batch.credentials-json`, `front.security.jwt-secret`/`shared-token`, `front.affiliation-partners.api-key`, `front.review-generation.api-key`, `captcha.key`/`secret-key`, `captcha-key`, `captcha-secret`, `feedback.github.access-token`, `feed.providers.*.api-key`, `google-api-json` (path or inline) | secret |

Prod-only keys not in beta: `api`'s `googlesearch.*`, `amazon-config.*`,
`urlfetcher.proxy.*`, `vertex.batch.credentials-json`, `spring.ai.openai.{embedding,image,audio,moderation}`;
`front-api`/`ui`'s `spring.boot.admin.client.{connect-timeout,read-timeout,period,register-once}`,
`spring.elasticsearch.{socket-timeout,connection-timeout}`; `ui`'s `google-api-json`.
Beta-only: `api`'s `review.generation.search-*` fields. These deltas are AC2
material (which of beta/prod's nine legacy secrets and four missing references
map to which key), not resolved further here.

## Key-level appendix: other mixed files

| File | Public keys | Secret keys |
|---|---|---|
| `configs/*/b2b-frontend/.env` | `NUXT_PORT`, `NUXT_PUBLIC_BACKEND_BASE_URL`, `NUXT_BACKEND_PROXY_TARGET` | none set (OIDC client secrets are commented placeholders) |
| `configs/*/frontend/.env` | `API_URL`, `NUXT_API_URL`, `NUXT_PUBLIC_API_BASE`, `HOTJAR_SITE_ID` (prod only) | `MACHINE_TOKEN`, `NUXT_MACHINE_TOKEN`, `HCAPTCHA_SITE_KEY` |
| `configs/beta/infra/.env` | `ES_HEAP` | `PLAUSIBLE_SECRET_KEY_BASE`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| `docker-compose/*/kibana.yml` | `server.host`, `server.publicBaseUrl`, `elasticsearch.username` | `elasticsearch.password` |
| `docker-compose/prod/server.xml`, `xwiki.cfg*` | connector/topology settings | embedded XWiki datasource credential |

## Findings outside AC1's scope (recorded, not acted on here)

- **Untracked local content, outside `git ls-files`**: the checkout audited here
  is the owner's existing local working copy (branch `security/rotate-xwiki-
  password`, identical to `origin/main`), not a disposable clone -- left
  as found, no writes made. It carries files `git status` reports as untracked:
  `doc/nudger/**` and three top-level `doc/*.pptx`/`*.pdf` (personal and
  administrative documents -- ACRE/URSSAF filings, family photos, a pitch
  deck), plus `root/etc/logrotate.d/`, `root/etc/systemd/` and `root/usr/`
  (provisioning artifacts, presumably from a local `prepare-box` run). None of
  this is a tracked legacy asset, so AC1's classification does not cover it,
  but the personal documents warrant flagging on their own: they sit inside a
  private repository this roadmap plans to eventually archive
  (`config-repository-retirement`), and their disposition is the owner's call,
  separate from this WorkOrder.
- **New secret exposure**: `docker-compose/beta/kibana.yml` line 3 holds a live
  `kibana_system` Elasticsearch password in plaintext. It surfaced in this
  session's tool output while confirming the file's key structure and is now in
  this session's transcript. Rotation is out of scope for AC1 (no production
  access authorized in this lot) and should be tracked as a new
  `leaked-credential-rotation` acceptance criterion or a sibling WorkOrder.
  Prod's copy is presumed to hold its own value at the same key; not read, to
  avoid repeating the exposure.
- **Possible stale token in documentation**: `root/root/README-prepare-box.md`
  and `root/root/open4goods-prepare/lib/args.sh`'s usage example both show a
  literal-shaped value for `--github-runner-token`, unlike the placeholder
  style used for the Cloudflare token example on the surrounding lines.
  GitHub runner registration tokens expire roughly one hour after issue, so
  this is very likely already dead, but it is a documentation hygiene item:
  replace it with a placeholder.
- **AC2 candidate gap**: `configs/prod/infra/.env` is 0 bytes while its beta
  counterpart carries four keys (`PLAUSIBLE_SECRET_KEY_BASE`, `ES_HEAP`,
  `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`). Either prod's infra stack reads
  these from elsewhere, or this is one of AC2's "four missing references."

## Out of scope for this pass

AC2 (environment secret/variable placement), AC3 (packaged non-secret
defaults) and AC4 (CI classification gate) are separate acceptance criteria in
the same WorkOrder and are not attempted here.
