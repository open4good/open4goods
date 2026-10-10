# Google SSO OAuth clients

Context: GOU-317. The Nuxt BFF exchanges the Google authorization code
**server-side** with a confidential OAuth **"Web"** client (`client_secret` +
PKCE as defense in depth). A "Desktop" client only supports loopback redirect
URIs and cannot be used once the redirect URI is `https://beta.nudger.fr/...`
or `https://nudger.fr/...`.

## Variables per environment

| App       | Variable                          | Purpose                                                              |
| --------- | ---------------------------------- | ---------------------------------------------------------------------- |
| frontend  | `GOOGLE_SSO_ENABLED`                | Master switch for the login button and the `/auth/google*` routes.     |
| frontend  | `GOOGLE_OAUTH_CLIENT_ID`            | OAuth "Web" client id. Not a secret, but must match front-api's value. |
| frontend  | `GOOGLE_OAUTH_REDIRECT_URI`         | Redirect URI for this environment, must be registered on the client.  |
| frontend  | `GOOGLE_OAUTH_CLIENT_SECRET`        | OAuth "Web" client secret. Server-only, never exposed to the browser.  |
| front-api | `FRONT_SECURITY_GOOGLE_SSO_ENABLED` | Enables the `/auth/google` ID-token verification endpoint.             |
| front-api | `FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID` | Expected audience when verifying the Google ID token.                |

All values are posed by Goulven in `open4goods-config`; this document never
lists real client ids, secrets, or fingerprints.

## Client id consistency

`FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID` (front-api) **must** carry the exact
same "Web" client id as `GOOGLE_OAUTH_CLIENT_ID` (frontend). front-api checks
the ID token audience (`aud`) against this value: a mismatch makes every
Google login fail the verification step even though the code exchange with
Google succeeded.

## Redirect URIs to register on the "Web" client

Register all of the following on the Google Cloud Console (project
`nudger-1711055953849`) for the "Web" OAuth client:

- `http://localhost:3000/auth/google/callback` (local dev, default Nuxt port)
- `http://127.0.0.1:4100/auth/google/callback` (agents' dev port range)
- `https://beta.nudger.fr/auth/google/callback`
- `https://nudger.fr/auth/google/callback` (kept registered but inactive: production stays
  `google-sso.enabled: false` until Goulven's go-ahead)

A "Web" client also accepts `http://localhost` redirect URIs, so the existing
local flow keeps working unchanged.

## Where the secret lives

`GOOGLE_OAUTH_CLIENT_SECRET` is deployed through `open4goods-config`, posed by
Goulven. It is never committed to this repository, never logged, and never
present in any `public` runtime config key.

## Rollout status

- Local dev: works with the "Web" client (code exchange includes
  `client_secret`).
- beta.nudger.fr: the frontend/front-api code is ready; the actual deployment
  depends on the beta environment rework (GOU-251), tracked separately.
- Production (nudger.fr): `google-sso.enabled` stays `false` until Goulven
  explicitly turns it on.
