# Infrastructure secrets, beta and prod (wave 3 runbook)

Context: credentials campaign GOU-1038, wave 3 (inventory entries 13 to 16 and
26 to 31), GOU-1092. Decision of Goulven (2026-10-23): rotate the
Elasticsearch passwords `elastic` and `kibana_system` now, despite the
Elasticsearch pause of 2026-10-11; the Kibana encryption key and the
Elasticsearch transport certificates with wave 3, in off-peak hours. The
default passwords of the Plausible Postgres and ClickHouse and of the b2b
Postgres, outside the inventory, were added by GOU-1256; their values are
generated under an approval of their own once that path is merged. The Google
OAuth client of Plausible (Search Console integration, inventory entry 27,
wave 2b of GOU-1298) was added by GOU-1321: nothing changes at Google, the
workflow only renders it and recreates Plausible.

This document never lists secret values, key ids or fingerprints. Application
secrets without a source to change (API keys, tokens) follow
[Service secrets](prod-service-secrets.md).

## What is rotated, and how

| Secret (GitHub Environment)         | Env          | Source changed by the workflow                    | Consumers (render)                                                                        |
| ----------------------------------- | ------------ | ------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `O4G_ELASTICSEARCH_PASSWORD`        | beta, prod   | Elasticsearch user `elastic`                      | api, ui, front-api, b2b-api: config tree `spring.elasticsearch.password`                  |
| `INFRA_KIBANA_SYSTEM_PASSWORD`      | beta, prod   | Elasticsearch user `kibana_system`                | Kibana: `/etc/open4goods/infra.env` -> `kibana.yml` `elasticsearch.password`              |
| `INFRA_KIBANA_ENCRYPTION_KEY`       | beta, prod   | none (the key in service becomes decryption-only) | Kibana: `infra.env` -> `xpack.encryptedSavedObjects.encryptionKey`                        |
| `INFRA_PLAUSIBLE_SECRET_KEY_BASE`   | beta         | none                                              | Plausible: `infra.env` -> `PLAUSIBLE_SECRET_KEY_BASE`                                     |
| `INFRA_PLAUSIBLE_PG_PASSWORD`       | beta         | Postgres user `postgres` of `plausible_db`        | Plausible: `infra.env` -> `PLAUSIBLE_PG_PASSWORD` (`DATABASE_URL`)                        |
| `INFRA_CLICKHOUSE_PASSWORD`         | beta         | ClickHouse user `default` of `plausible_events_db` (set from its env at every start) | ClickHouse and Plausible: `infra.env` -> `CLICKHOUSE_PASSWORD` |
| `INFRA_GOOGLE_CLIENT_ID`, `INFRA_GOOGLE_CLIENT_SECRET` | beta | none (Google OAuth client of the Search Console integration, issued by Google) | Plausible: `infra.env` -> `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| `B2B_API_POSTGRES_PASSWORD`         | beta, prod   | Postgres user of `b2b_postgres` (`product_data_api`) | b2b-api: config tree `spring.datasource.password`; `infra.env` -> `B2B_POSTGRES_PASSWORD` |
| Elasticsearch transport certificates | beta, prod  | regenerated on the host by Ops                    | Elasticsearch (see [Transport certificates](#transport-certificates-30))                  |

Access generates every value with `secret_generate` `kind=hex32`: the workflow
refuses a value outside `[A-Za-z0-9._-]` or shorter than 32 characters (it
travels through a JSON body, an SQL literal and a compose env file). `beta`
and `prod` get distinct values. `beta` already holds a stale
`INFRA_PLAUSIBLE_SECRET_KEY_BASE` (one of the 61 secrets nothing reads), which
the check refuses: Access overwrites it (`overwrite: true`).

Exception: Google issues the OAuth client of Plausible, nothing generates it.
Access copies into `INFRA_GOOGLE_CLIENT_ID` and `INFRA_GOOGLE_CLIENT_SECRET`
the client in service (`configs/beta/infra/.env` of `open4goods-config`), or a
new client created in the Google Cloud console (`overwrite: true`: `beta`
holds stale values of 2026-09-15 under these names). The check refuses a
client ID that is not `<digits>-<id>.apps.googleusercontent.com` and a secret
outside `[A-Za-z0-9_-]` or shorter than 24 characters, then asks Google's
token endpoint (`https://oauth2.googleapis.com/token`) to exchange a random
authorization code with the pair: `invalid_grant` (or
`redirect_uri_mismatch`) means Google authenticated the client;
`invalid_client` or `deleted_client` stops the workflow (a stale or deleted
client), as does no answer. The request body goes through a mode 600 file,
never a command line; only Google's error code is printed.

Not rotated by this workflow:

- `O4G_REDIS_PASSWORD` (#14): no deployed service reads Redis any more. api
  and ui have no Redis client (the dependency of `commons` is commented out),
  so `spring.data.redis.*` of their `application-active.yml` is dead. Only
  b2b-api uses Redis: its own `b2b_redis` container, bound to localhost,
  without a password. Ops checks whether the Redis named in those files
  (`nudger.fr:6379` in prod, `beta2.nudger.fr:6379` in beta) still runs and
  who connects to it (`CLIENT LIST`); if nobody does, stop it. If it must stay,
  set a new `requirepass` by hand (`CONFIG SET requirepass`, then
  `CONFIG REWRITE`), stored nowhere since nothing reads it. Step d removes
  `spring.data.redis.password` from the api and ui configurations.
- `O4G_XWIKI_PASSWORD` (#15, user `o4g`) and `ADMIN_XWIKI_PASSWORD` (#16,
  user `nudger`): no class reads `xwiki.*` since the XWiki decommission
  (GOU-56, GOU-922), so nothing is rendered. XWiki itself is dismantled in
  prod (GOU-916, GOU-1334): there is no step to do in XWiki any more; step d
  only removes the `xwiki:` blocks.

## Single definition and files on the host

- `scripts/deploy/render-service-secrets.sh` holds the rows, with the need
  `source`: they are only rendered with `--source`, which renders nothing
  else. Deploys and `serviceSecrets.yml` never render them, so a value written
  in the GitHub Environment before the source accepts it cannot break a
  service at the next deploy. The `infra` service of that script writes
  `/etc/open4goods/infra.env` (mode 600).
- `/etc/open4goods/infra.env` is outside every path that an
  `open4goods-config` deploy writes (`/opt/open4goods/bin/`, `latest/`,
  `config/`), and `publish-jars.sh` does not source it: the Java services
  never see the infrastructure secrets. Next to the compose file
  (`/opt/open4goods/bin/`) it would be deleted by the `rsync --delete` of
  `deployConfiguration.yml`; in `config/<env>/infra/.env` it would be
  rewritten by every deploy and sourced by every Java service.
- A value in service that `infra.env` must carry and that falls outside
  `[A-Za-z0-9/._-]` (the Kibana key of `open4goods-config` holds `$` and
  punctuation) is written single-quoted: compose takes it literally, `$`
  included. A quote or a newline cannot be carried: the workflow stops.
- `/etc/open4goods/infra-previous.env` (mode 600): before changing a source,
  the workflow keeps there the value in service, for `rollback`. It is the
  only copy outside `open4goods-config`; Ops deletes it at step c.

## Workflow `infraSecrets.yml`

Actions > "Infra secrets (source + render + restart)" > Run workflow.
Inputs: `env` (`beta`, `prod`), `target` (`elasticsearch`, `kibana`,
`plausible`, `plausible-postgres`, `plausible-clickhouse` and
`plausible-google` for beta, `b2b-postgres`), `mode` (`test`, `apply`,
`rollback`).
`apply` and `rollback` only run on `main`; `test` writes nothing and may run
from a branch. The job shares the concurrency group of the deploys
(`deploy-prod`, `deploy-beta-backend`) without cancelling them.

| Step                                                     | `test` | `apply` | `rollback` |
| -------------------------------------------------------- | ------ | ------- | ---------- |
| Check the new values (no SSH)                            | yes    | yes     | no         |
| Check the host and the value in service (read only)      | yes    | yes     | no         |
| Change the password at its source                        | no     | yes     | no         |
| Render (`render-service-secrets.sh --source`)            | no     | yes     | no         |
| Restart the consumers                                    | no     | yes     | no         |
| Put the previous value back (source, render, restart)    | no     | no      | yes        |

How the value in service is found: by authenticating with each candidate,
never by printing it. Elasticsearch `elastic`: the config trees of the four
consumers, their `application-active.yml`, the kept previous value.
`kibana_system` and the Kibana key: `infra.env`, `kibana.yml` of the host, the
`kibana.yml` the running container sees. Plausible
Postgres and ClickHouse: `infra.env`, `config/beta/infra/.env`, the default of
the compose placeholder (`${VAR:-...}`), the URL the running Plausible was
created with, the environment of the database container. b2b Postgres: the
b2b-api config tree, `infra.env`, the three env files `publish-jars.sh`
sources for b2b-api, the default of the placeholder in its
`application-active.yml` and in the compose file, the environment of
`b2b_postgres`. In every case the kept previous value comes last. The Google
OAuth client has no source to authenticate against on the host: its value in
service is the first set of `infra.env`, `config/beta/infra/.env` and the
environment of the running `plausible` container, compared with the new one
and never printed (the runner check asked Google beforehand).

The official Postgres image trusts its socket and loopback connections:
authentication goes over TCP to the container's network address, where
`pg_hba.conf` checks the password, and the workflow first checks that a
random wrong password is refused (otherwise it stops: the value in service
cannot be found that way). Same check for ClickHouse, whose client gets the
password from a configuration file streamed into the container.

Idempotence: the source step skips a change the source already accepts, never
overwrites a kept previous value, and never turns the new Kibana key into the
decryption-only one; the restart step refuses a render that does not hold the
new value. After a partial failure, run `apply` again.

What each target does:

- `elasticsearch`: changes `elastic` (`POST _security/user/elastic/_password`,
  authenticated with the value in service), renders the config tree of
  front-api, api, ui and b2b-api, then restarts them in that order with
  `publish-jars.sh <env> restart <service>`: front-api first, because it
  answers the site and only needs the running api to answer. Between the
  change and each restart, that service's Elasticsearch calls fail: the
  outage is the time of the rolling restart (a few minutes for api).
- `kibana`: keeps the key in service as `INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY`
  in `infra.env`, changes `kibana_system` (as `elastic`), renders `infra.env`,
  recreates Kibana only (`INFRA_SERVICES=kibana publish-infra.sh <env>`),
  waits for it, then calls
  `POST /api/encrypted_saved_objects/_rotate_key` (counts only in the log).
  Kibana is down between the password change and its restart.
- `plausible` (beta): reports the users with TOTP enabled (Plausible derives
  its TOTP vault key from `SECRET_KEY_BASE` when `TOTP_VAULT_KEY` is not set:
  their second factor must be reset), renders `infra.env` (it overrides
  `config/beta/infra/.env`, read first), recreates Plausible. Every session is
  invalidated. It also reports the count and names of the API keys (never
  their hash): **all Stats API keys are invalidated** (401), because Plausible
  hashes each key with `SECRET_KEY_BASE` (`sha256(secret_key_base <> key)`).
  Known consumers: the Paperclip connection "Plausible (Nudger)" (Paperclip
  secret `PLAUSIBLE_STATS_API_KEY`) and Metriks (`PLAUSIBLE_STATS_API_TOKEN`).
  After the rotation: create one key per consumer in the Plausible UI, set it
  at each consumer, then delete the dead keys.
- `plausible-postgres` (beta): changes `postgres` of `plausible_db`
  (`ALTER USER`, over TCP, authenticated with the value in service), renders
  `PLAUSIBLE_PG_PASSWORD` in `infra.env`, recreates Plausible only.
  `plausible_db` keeps running (`POSTGRES_PASSWORD` is only read when its data
  directory is created). Between the change and the restart, Plausible's
  existing connections keep working; a new one fails.
- `plausible-clickhouse` (beta): ClickHouse does not store this password:
  the entrypoint of the image writes `CLICKHOUSE_PASSWORD` to
  `users.d/default-user.xml` at every start. `source` only keeps the value in
  service; `restart` recreates `plausible_events_db` (it then accepts the new
  value), waits for it, recreates the dictionaries (below), then recreates
  Plausible and checks that it writes.
  - Dictionaries: Plausible's migrations create dictionaries
    (`location_data_dict`, `acquisition_channel_*_dict`...) whose source is
    this ClickHouse, with the password of the moment written in their DDL
    (`SOURCE(CLICKHOUSE(... USER 'default' PASSWORD '...'))`). Insertions into
    `events_v2` read them: after a password change they fail with
    `Code: 516 Authentication failed` (Plausible logs
    `Plausible.Event.WriteBuffer ... terminating` every few seconds, ClickHouse
    `ExternalDictionariesLoader: Could not load external dictionary`), while
    `/api/health` still answers 200. Neither a restart nor Plausible's
    migrations update them (GOU-1268). Before Plausible is recreated, every
    dictionary of Plausible's database (the path of its
    `CLICKHOUSE_DATABASE_URL`) whose source is this ClickHouse as `default`
    with a `PASSWORD` is recreated from its `SHOW CREATE DICTIONARY`
    (ClickHouse shows the password as `[HIDDEN]`), only its `PASSWORD` literal
    replaced, with `CREATE OR REPLACE DICTIONARY`, then
    `SYSTEM RELOAD DICTIONARY`; each must then be `LOADED` in
    `system.dictionaries`. The DDL goes through files of the temp directory
    and stdin, never a command line, and is never printed. The other
    dictionaries (another source, user or host, no password) are listed by
    `test` and left alone. Re-running `restart` recreates them identical.
  - Write check: after `/api/health`, no `Code: 516` line in
    `docker logs plausible --since <its restart>` for 45 s (9 checks 5 s
    apart, `PLAUSIBLE_WRITE_ATTEMPTS`), then the number of events written to
    `events_v2` since the restart (for information: 0 without traffic). A
    dictionary that is not `LOADED` (Plausible is then not recreated) or a
    `Code: 516` line fails the run with a pointer to `mode=rollback`.
  - `test` lists the dictionaries `restart` would recreate, by name and status
    (`system.dictionaries`), flags one already failing, and names the others.
  - Lost events: those sent between the recreation of `plausible_events_db`
    and the first write of the new Plausible (a minute or two on beta).
  - No dictionary, table or database of Plausible's ClickHouse reads Postgres
    (Plausible CE migrations): `plausible-postgres` needs no such step.
- `plausible-google` (beta): `source` changes nothing at Google; it keeps
  each value in service that differs from the new one
  (`INFRA_GOOGLE_CLIENT_ID`, `INFRA_GOOGLE_CLIENT_SECRET` in
  `infra-previous.env`). The render writes `GOOGLE_CLIENT_ID` and
  `GOOGLE_CLIENT_SECRET` in `infra.env`, which overrides
  `config/beta/infra/.env` (the compose file already reads
  `${GOOGLE_CLIENT_ID:-}`: no `open4goods-config` change is needed before it).
  `restart` recreates Plausible only, waits for `/api/health`, then checks
  that the new container runs with both new values. Sessions and the
  `SECRET_KEY_BASE` are untouched. With the client already in service (the
  usual case), nothing is kept and the restart is the only effect; with
  another client, `test` warns that the Search Console link of each site must
  be redone in Plausible (Site settings > Integrations), as the OAuth tokens
  Plausible holds belong to the former client.
- `b2b-postgres` (beta, prod): changes the user of `b2b_postgres`
  (`ALTER USER`), renders `B2B_POSTGRES_PASSWORD` in `infra.env` (for the
  compose file) and the config tree `spring.datasource.password` of b2b-api,
  then restarts b2b-api (`publish-jars.sh <env> restart b2b-api`).
  `b2b_postgres` keeps running. `test` refuses an env file that sets
  `SPRING_DATASOURCE_PASSWORD`, which would beat the tree, and a running JAR
  that is not the latest. The tree beats the `B2B_POSTGRES_PASSWORD`
  placeholder of `application-active.yml`, even when an env file sets it.
  This target does not need step d-infra.

If Postgres refuses an `ALTER USER`, its log may hold the statement, so the new
value too (`log_min_error_statement`): regenerate that value before the next
`apply`.

### Where these three databases are exposed

None is reachable from outside the host. `b2b_postgres` publishes its port on
the host loopback only (`127.0.0.1:5433` in beta, `127.0.0.1:5432` in prod):
any process on the host can try the default password. `plausible_db` and
`plausible_events_db` publish no port: only the containers of the compose
network `open4goods_network` reach them (Plausible, Elasticsearch, Kibana,
b2b). The defaults are short, and every reader of `open4goods-config` has them:
rotate them with wave 3, without an emergency window.

### Prerequisite: open4goods-config step d-infra

The compose files, `kibana.yml` and `publish-infra.sh` belong to
`open4goods-config`. Before the `kibana`, `plausible`,
`plausible-postgres` and `plausible-clickhouse` targets (not before
`elasticsearch` nor `b2b-postgres`), a PR on `open4goods-config` (Dev, ordinary gate) makes
them read `infra.env`, deployed with `deployConfiguration.yml` for that
environment. The `test` mode fails with "deploy open4goods-config step d-infra
first" until then.

1. `root/opt/open4goods/bin/publish-infra.sh`:
   - adds `--env-file /etc/open4goods/infra.env` after
     `--env-file config/<env>/infra/.env` when the file exists (later files
     win);
   - with `INFRA_SERVICES` set (space-separated compose services), copies the
     files as today, then only runs
     `up -d --no-deps --force-recreate $INFRA_SERVICES`, without the full
     `down`;
   - runs `docker compose ... config -q` before any `down`, so that a missing
     variable stops it before the infrastructure goes down.
2. `docker-compose/{beta,prod}/docker-compose.infra.yml`, service `kibana`:
   `environment` passes `INFRA_KIBANA_SYSTEM_PASSWORD`,
   `INFRA_KIBANA_ENCRYPTION_KEY` and `INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY`
   (`${VAR:?rendered by infraSecrets.yml}`).
3. `docker-compose/{beta,prod}/kibana.yml` (Kibana reads `${VAR}` from its
   environment):

   ```yaml
   elasticsearch.password: ${INFRA_KIBANA_SYSTEM_PASSWORD}
   xpack.encryptedSavedObjects:
     encryptionKey: ${INFRA_KIBANA_ENCRYPTION_KEY}
     keyRotation:
       decryptionOnlyKeys: ["${INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY}"]
   ```

Nothing else for the Plausible Postgres, ClickHouse and the b2b Postgres: the
compose files already read `PLAUSIBLE_PG_PASSWORD`, `CLICKHOUSE_PASSWORD` and
`B2B_POSTGRES_PASSWORD`, the names `infra.env` carries; the literal defaults
leave at step d.

Merge it inside the wave 3 window of that environment, right before the
`kibana` apply: a beta push deploys it at once (unless
`BETA_DEPLOY_FREEZE`), and from then on a full `publish-infra.sh` needs
`infra.env` (the `config -q` check refuses to stop the infrastructure without
it). Once deployed, the literal values are no longer used: steps 3 and 4
remove them from `open4goods-config`.

## Wave 3 order, per environment (beta first, then prod)

Off-peak hours for prod. Each line is one run of `infraSecrets.yml`, first
with `mode=test`, then `mode=apply`.

1. Access writes the environment's secrets (`kind=hex32`).
2. `elasticsearch`: no prerequisite. Proof: the run summary, the four health
   checks of `publish-jars.sh`, a product page and a search on the site.
3. Merge and deploy open4goods-config step d-infra for that environment.
4. `kibana`. Proof: Kibana login page and `/api/status` available, the
   re-encryption counts (`failed` = 0), an existing saved object (dashboard,
   connector) opens.
5. beta: `plausible`. Proof: Plausible login, a dashboard loads; TOTP users
   reset their second factor; recreate the Stats API keys and set them at
   their consumers (see `plausible` above), proof: the Growth connector answers.
   Then, once Access wrote their values (approval of their own, GOU-1256):
   beta `plausible-postgres`, then `plausible-clickhouse` (proof:
   `/api/health` of Plausible, its dictionaries `LOADED` and no `Code: 516`
   in the run log, new events in `events_v2`, a dashboard shows today's
   visits);
   `b2b-postgres` in beta, then in prod (proof: the b2b-api health check of
   `publish-jars.sh`, a call of the Product Data API that reads the ledger).
   Their previous values join `infra-previous.env`: step c deletes it after
   these proofs too.
6. Transport certificates (Ops, below), in the same window.
7. Step c: Ops deletes `/etc/open4goods/infra-previous.env`; Access deletes
   the stale `beta` secrets with an old name. Step d (Dev, PR on
   `open4goods-config`): see below.

Stops: a `test` that fails means no `apply`, back to the CTO. A consumer down
after `apply`: `mode=rollback` for that target.

## Wave 2b: the Google OAuth client of Plausible (beta)

GOU-1298 (#27), path added by GOU-1321. In the wave 2b window:

1. Access writes `INFRA_GOOGLE_CLIENT_ID` and `INFRA_GOOGLE_CLIENT_SECRET` in
   `beta` (`overwrite: true`), with the client in service unless a new one is
   decided.
2. `plausible-google`, `mode=test`: Google accepts the client, both values in
   service are found; "already in service" for both when Access copied them.
3. `mode=apply`. Proof: `/api/health` of Plausible in the run, then Site
   settings > Integrations of a site shows Search Console connected and its
   dashboard loads the search terms.
4. Step d for this client (Dev, PR on `open4goods-config`, merged once the
   proof is in): `configs/beta/infra/.env` loses `GOOGLE_CLIENT_ID` and
   `GOOGLE_CLIENT_SECRET`, the header comment of
   `docker-compose/beta/docker-compose.infra.yml` its example values; the
   placeholders may become `${GOOGLE_CLIENT_ID:?rendered by infraSecrets.yml}`
   (services `plausible_migrate` and `plausible`), so that a full
   `publish-infra.sh` without the render stops instead of starting Plausible
   without Search Console.

Rollback: `mode=rollback` puts the kept client back in `infra.env` and
recreates Plausible.

## Rollback

`mode=rollback` reads `/etc/open4goods/infra-previous.env` and puts the
previous value back at the source (authenticated with the new one), in the
rendered files (config trees, `infra.env`), then restarts the consumers
(`plausible-clickhouse`: its dictionaries are recreated with the previous
value too, and the same write check runs; `plausible-google`: `infra.env`
and Plausible only, nothing at Google). For
the Kibana key, the previous key is primary again and the new one stays
decryption-only, so objects re-encrypted with it remain readable. The GitHub
Environment keeps the new value: fix or regenerate it before the next `apply`.
Until step d, the previous values also remain in the `open4goods-config`
files on the host.

## Transport certificates (#30)

Recommendation (confirmed): the `.p12` files are regenerated **on the host**
by Ops, and go neither to GitHub nor to `open4goods-config`. They have no
password today (`elasticsearch.yml` sets none and the Elasticsearch keystore
is not persisted, as `config/` is not a volume): keep it that way and protect
the file by its permissions; a password would need a persisted keystore. The
cluster has one node; `discovery.type: single-node` would remove the need for
transport TLS altogether, but changes the cluster configuration during the
Elasticsearch pause: left for later.

Off-peak, per environment, on the host (Ops):

```sh
# 1. New CA and node certificate, inside the running container (no password).
docker exec es01 bin/elasticsearch-certutil ca --silent --pass "" --out /tmp/ca.p12
docker exec es01 bin/elasticsearch-certutil cert --silent --ca /tmp/ca.p12 --ca-pass "" --pass "" --out /tmp/node.p12
docker cp es01:/tmp/node.p12 /opt/open4goods/bin/elastic-certificates.p12.new
docker exec es01 rm -f /tmp/ca.p12 /tmp/node.p12
# 2. Replace in place (same inode: the bind mount sees it), readable by the Elasticsearch user (uid 1000, gid 0) only.
cp /opt/open4goods/bin/elastic-certificates.p12 /opt/open4goods/bin/elastic-certificates.p12.old
cat /opt/open4goods/bin/elastic-certificates.p12.new > /opt/open4goods/bin/elastic-certificates.p12
docker run --rm -v /opt/open4goods/bin:/mnt alpine sh -c \
  'chown 1000:0 /mnt/elastic-certificates.p12 && chmod 640 /mnt/elastic-certificates.p12'
# 3. Restart Elasticsearch, then check.
docker restart es01
```

Proof: `GET _cluster/health` green or yellow, `elastic` authenticates, the
Spring services' health checks and a search on the site. Rollback: write
`elastic-certificates.p12.old` back into the file through the same docker
helper (the file belongs to uid 1000 after step 2) and restart `es01`. Then
delete `.new` and `.old`, and `elastic-stack-ca.p12` from
`/opt/open4goods/bin/` (no container mounts it).

Until step d removes them from `open4goods-config`, a `publish-infra.sh` run
copies the repository's `.p12` back over the new one: do not run it between
this step and step d. Step d removes the `.p12` files and their `copy_file`
lines; `deployConfiguration.yml` already excludes `/*.p12` from its
`--delete`, so the host files stay.

## Step d: what leaves open4goods-config

After the proofs of both environments (Dev, PR on `open4goods-config`,
ordinary gate):

- `configs/{beta,prod}/{api,ui,front-api,b2b-api}/application-active.yml`:
  `spring.elasticsearch.password` (the config tree replaces it).
- `configs/{beta,prod}/{api,ui}/application-active.yml`:
  `spring.data.redis.password` (and the whole dead `spring.data.redis` block).
- `configs/{beta,prod}/{api,ui,front-api,admin}/application-active.yml`: the
  `xwiki:` blocks (`o4g`, `nudger`).
- `docker-compose/{beta,prod}/kibana.yml`: the literal password and key
  (already replaced by `${...}` in step d-infra).
- `configs/beta/infra/.env`: `PLAUSIBLE_SECRET_KEY_BASE`; the compose default
  of `PLAUSIBLE_SECRET_KEY_BASE` becomes `${PLAUSIBLE_SECRET_KEY_BASE:?}`.
- After the `plausible-google` proof (wave 2b, above): `configs/beta/infra/.env`,
  `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, and the example values of the
  header comment of `docker-compose/beta/docker-compose.infra.yml`.
- After the `plausible-postgres`, `plausible-clickhouse` and `b2b-postgres`
  proofs: `docker-compose/beta/docker-compose.infra.yml`, the literal
  defaults of `PLAUSIBLE_PG_PASSWORD`, `CLICKHOUSE_PASSWORD` (services
  `plausible_db`, `plausible_events_db`, `plausible_migrate`, `plausible`) and
  `B2B_POSTGRES_PASSWORD` become `${VAR:?rendered by infraSecrets.yml}`, and
  the header comment loses its example values;
  `docker-compose/prod/docker-compose.infra.yml`, `B2B_POSTGRES_PASSWORD`
  likewise; `configs/{beta,prod}/b2b-api/application-active.yml`, the default
  of `${B2B_POSTGRES_PASSWORD:...}` and the example value of its comment.
- `docker-compose/{beta,prod}/elastic-certificates.p12`,
  `elastic-stack-ca.p12`, and their `copy_file` lines in `publish-infra.sh`.

Outside the manifest, noticed while preparing wave 3 and rotated by the
`plausible-postgres`, `plausible-clickhouse` and `b2b-postgres` targets
(GOU-1256): the literal defaults of `PLAUSIBLE_PG_PASSWORD`,
`CLICKHOUSE_PASSWORD` and `B2B_POSTGRES_PASSWORD` in the compose files and the
b2b-api `application-active.yml`, which are the passwords in service.
