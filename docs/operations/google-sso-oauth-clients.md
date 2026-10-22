# Google SSO OAuth clients

Context: GOU-317. The Nuxt BFF exchanges the Google authorization code
**server-side** with a confidential OAuth **"Web"** client (`client_secret` +
PKCE as defense in depth). A "Desktop" client only supports loopback redirect
URIs and cannot be used once the redirect URI is `https://beta.nudger.fr/...`
or `https://nudger.fr/...`.

## Variables per environment

The frontend is deployed as a built Nuxt artifact
(`node .output/server/index.mjs`): Nuxt resolves `runtimeConfig` expressions
**at build time**, so a bare env var (e.g. `GOOGLE_SSO_ENABLED`) read in
`nuxt.config.ts` is frozen into the artifact during `pnpm build` and has
**no effect** when set on the running container afterwards. Only the
`NUXT_`-prefixed names below are read by Nuxt's runtime config override
mechanism and are safe to set on an already-built deployment; they are the
names to pose in the frontend's runtime environment (container env, beta,
etc.).

| App       | Variable (runtime, pose this)         | Purpose                                                              |
| --------- | -------------------------------------- | ---------------------------------------------------------------------- |
| frontend  | `NUXT_PUBLIC_GOOGLE_SSO_ENABLED`       | Master switch for the login button and the `/auth/google*` routes.     |
| frontend  | `NUXT_GOOGLE_OAUTH_CLIENT_ID`          | OAuth "Web" client id. Not a secret, but must match front-api's value. |
| frontend  | `NUXT_GOOGLE_OAUTH_REDIRECT_URI`       | Redirect URI for this environment, must be registered on the client.  |
| frontend  | `NUXT_GOOGLE_OAUTH_CLIENT_SECRET`      | OAuth "Web" client secret. Server-only, never exposed to the browser.  |
| front-api | `FRONT_SECURITY_GOOGLE_SSO_ENABLED`    | Enables the `/auth/google` ID-token verification endpoint.             |
| front-api | `FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID`  | Expected audience when verifying the Google ID token.                |

The frontend also reads unprefixed `GOOGLE_SSO_ENABLED` / `GOOGLE_OAUTH_*`
build-time fallbacks (useful for local dev run via `pnpm dev`, which
re-evaluates `nuxt.config.ts` on every start), but these have no effect once
the app is built: always use the `NUXT_*` names above for a deployed
artifact.

Switches and redirect URIs stay in `open4goods-config`. The client id and
secret come from the `beta` GitHub Environment on beta (see
[Beta source](#beta-source-github-environment)) and from the `prod`
GitHub Environment in production (see
[Production source](#production-source-github-environment)). This document
never lists real client ids, secrets, or fingerprints.

## Beta source: GitHub Environment

Decision of 2026-10-20 (GOU-501, GOU-767, ADR-0006): on beta, the Google
"Web" client comes from the `beta` GitHub Environment of
`open4good/open4goods`, the single source of the client id:

| Environment secret          | Rendered as                          | File on the beta host                          | Rendered by                                  |
| --------------------------- | ------------------------------------ | ---------------------------------------------- | -------------------------------------------- |
| `GOOGLE_SSO_CLIENT_ID`      | `NUXT_GOOGLE_OAUTH_CLIENT_ID`        | `/opt/open4goods/config/beta/frontend/.env`    | `frontend-ci.yml`, before `publish-frontend.sh` |
| `GOOGLE_SSO_CLIENT_SECRET`  | `NUXT_GOOGLE_OAUTH_CLIENT_SECRET`    | `/opt/open4goods/config/beta/frontend/.env`    | `frontend-ci.yml`, before `publish-frontend.sh` |
| `GOOGLE_SSO_CLIENT_ID`      | `FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID` | `/etc/open4goods/front-api.env`               | `testAndPublishBeta.yml`, before `publish-jars.sh` |

`scripts/deploy/push-remote-env.sh` replaces only these keys in the target
file: values travel through SSH stdin (never a command line, an artifact or a
log), the file is rebuilt atomically with mode 600. These keys are no longer
in `open4goods-config` `configs/beta/*`.

- front-api: `publish-jars.sh` sources `/etc/open4goods/front-api.env`, which
  sits outside the tree that `open4goods-config`'s deploy rsyncs with
  `--delete`, so it survives config deploys and `publishJars.yml` restarts.
  The OS environment variable also beats `application-active.yml`.
- frontend: the compose file (owned by `open4goods-config`) only reads
  `config/beta/frontend/.env`, which every config deploy rewrites without
  these keys. Running containers keep their environment; recreating them
  outside `frontend-ci.yml` (manual `publish-frontend.sh`) after a config
  deploy starts them without the client. Redeploy through `frontend-ci.yml`.

## Production source: GitHub Environment

Decision of 2026-10-21 (GOU-255, GOU-826, ADR-0006): in production, the same
Google "Web" client as beta comes from the `prod` GitHub Environment of
`open4good/open4goods`. The `release` job of `releaseDeployProd.yml` declares
`environment: prod`:

| Environment secret          | Rendered as                          | File on the prod host                          | Rendered by                                  |
| --------------------------- | ------------------------------------ | ---------------------------------------------- | -------------------------------------------- |
| `GOOGLE_SSO_CLIENT_ID`      | `NUXT_GOOGLE_OAUTH_CLIENT_ID`        | `/opt/open4goods/config/prod/frontend/.env`    | `releaseDeployProd.yml`, before `publish-frontend.sh` |
| `GOOGLE_SSO_CLIENT_SECRET`  | `NUXT_GOOGLE_OAUTH_CLIENT_SECRET`    | `/opt/open4goods/config/prod/frontend/.env`    | `releaseDeployProd.yml`, before `publish-frontend.sh` |
| `GOOGLE_SSO_CLIENT_ID`      | `FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID` | `/etc/open4goods/front-api.env`               | `releaseDeployProd.yml`, before `publish-jars.sh` |

The rendering steps use `scripts/deploy/push-remote-env.sh` (same guarantees
as beta) and only run with `mode=release`. The first step of the job, "Check
Google SSO secrets", runs in every mode, including `mode=test`: it fails when
either secret is empty or holds characters the script would reject, without
any SSH access and without printing a value. A `mode=test` rehearsal therefore
proves the `prod` Environment is ready.

The rendering steps run the script of the deployed ref. A step right after
the checkout, also run in every mode, fails when that ref has no
`scripts/deploy/push-remote-env.sh`: deploy a ref cut after GOU-767 (tags up
to v1.0.61 do not contain it).

Other production secrets, rendered by the same release and by the
dispatchable `prodSecrets.yml`, are listed in
[Production service secrets](prod-service-secrets.md).

The same caveats as beta apply: `/etc/open4goods/front-api.env` survives
config deploys, while `config/prod/frontend/.env` is rewritten by every
`open4goods-config` deploy, so recreate the frontend containers through
`releaseDeployProd.yml` rather than a manual `publish-frontend.sh`.

## Client id consistency

`FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID` (front-api) **must** carry the exact
same "Web" client id as `NUXT_GOOGLE_OAUTH_CLIENT_ID` (frontend). front-api checks
the ID token audience (`aud`) against this value: a mismatch makes every
Google login fail the verification step even though the code exchange with
Google succeeded.

## Role allowlist (front-api)

Context: GOU-945, GOU-949. Once the ID token is verified, front-api grants
roles only to the e-mail addresses listed in
`front.security.google-sso.role-allowlist` (deny by default). The allowlist
lives in `open4goods-config`, next to the `google-sso.enabled` switch.

Each key is an e-mail address and **must be quoted and bracketed** in YAML:

```yaml
front:
  security:
    google-sso:
      role-allowlist:
        "[john.doe@example.com]": [ROLE_SITEEDITOR]
        "[jane.roe@example.org]": [ROLE_SITEEDITOR, XWIKIADMINGROUP]
```

Without brackets (`john.doe@example.com: [...]`), Spring Boot drops the `@`
while binding the map key: the entry becomes `john.doeexample.com`, never
matches a login, and every Google login of that user gets a 401.

- Keys are lower-cased on binding; the lookup ignores case.
- At startup, when Google SSO is enabled, front-api logs an `ERROR` for every
  key without `@` (masked, e.g. `j***`) and reminds the `"[adresse@domaine]"`
  notation. It logs a `WARN` when the allowlist is empty. The application
  still starts, so a bad allowlist does not take the rest of front-api down.
- Every refused Google login logs a `WARN`
  `Google SSO login refused (401): <reason>` (issuer, audience, nonce,
  unverified e-mail, token verification failure, or e-mail absent from the
  allowlist). The ID token and the nonce are never logged; the e-mail is
  masked (`j***@example.com`).

## Redirect URIs to register on the "Web" client

Register all of the following on the Google Cloud Console (project
`nudger-1711055953849`) for the "Web" OAuth client:

- `http://localhost:3000/auth/google/callback` (local dev, default Nuxt port)
- `http://127.0.0.1:4100/auth/google/callback` (agents' dev port range)
- `https://beta.nudger.fr/auth/google/callback`
- `https://nudger.fr/auth/google/callback`

A "Web" client also accepts `http://localhost` redirect URIs, so the existing
local flow keeps working unchanged.

## Where the secret lives

`NUXT_GOOGLE_OAUTH_CLIENT_SECRET` comes from the `GOOGLE_SSO_CLIENT_SECRET`
secret of the `beta` GitHub Environment on beta and of the `prod` GitHub
Environment in production. It is never committed to this repository, never logged, and never
present in any `public` runtime config key.

## Rollout status

- Local dev: works with the "Web" client (code exchange includes
  `client_secret`).
- beta.nudger.fr: client id and secret rendered from the `beta` GitHub
  Environment at deploy time (GOU-767).
- Production (nudger.fr): client id and secret rendered by
  `releaseDeployProd.yml` from the `prod` GitHub Environment (GOU-826).
  The `google-sso.enabled` switches stay in `open4goods-config`.
