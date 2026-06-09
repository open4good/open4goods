# Product Data API - Authentication, OIDC & authorization

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> Two distinct auth surfaces: **dashboard/admin** (human, OIDC + JWT/session) and
> **external data API** (machine, opaque `pdapi_` key). Reuse Infera's verifier
> services as references (see paths below); they are the closest in-house pattern.

## 1. External data API auth (`pdapi_` keys)

- Format: `pdapi_` + sufficient random entropy (>= 32 bytes base32, URL-safe).
  Presented as `Authorization: Bearer pdapi_...`.
- Storage: only `key_prefix` (e.g. `pdapi_a1b2c3d4`, shown in lists) and
  `key_hash` = SHA-256 hex of the full clear key. The clear key is returned
  **only** from the create and rotate responses, never again.
- Lookup: incoming key -> SHA-256 -> `b2b:apikey:{sha256}` Redis cache (TTL),
  Postgres `api_keys` fallback by `key_hash`. Resolve to `{orgId, keyId, status}`.
- Reject `401` if header missing, malformed, unknown, `REVOKED`, `ROTATED`, or the
  owning org is `SUSPENDED`/`CLOSED`.
- `last_used_at`: written debounced/async (`b2b:lastused:{keyId}`, flushed by
  `HardenerBatch`) - never block the request on it.
- Rotation: create a new key, mark the old `ROTATED` (`rotated_from` lineage),
  return the new clear key once. Revocation: set `status=REVOKED`,
  `revoked_at=now()`, evict the Redis cache entry.

Implementation: `ApiKeyAuthFilter extends OncePerRequestFilter` placed before the
authorization filter; sets an `Authentication` carrying `orgId`/`keyId`.
`SecurityConfig` modelled on `front-api/.../config/WebSecurityConfig.java`
(CSRF off for the stateless API, CORS from config). Public paths: `/v3/api-docs/**`,
`/swagger-ui/**`, Redoc, `/actuator/health`, `/api/v1/auth/**`. Everything under
`/api/v1/products/**` requires a valid key.

## 2. Dashboard / admin auth (OIDC)

Endpoint `POST /api/v1/auth/oidc` accepts `{ provider, idToken }` (or auth code,
per provider) and:

1. Verifies the token (per-provider mechanics below).
2. Provisions/updates the `users` row (`oidc_provider`, `oidc_subject`, email,
   display name, avatar).
3. Creates a default `organizations` row + `organization_members` (`OWNER`) for
   first-time users, and applies the one-time free 2500-credit grant.
4. Computes `is_platform_admin` from the configurable admin-email allowlist.
5. Issues an access JWT (short-lived) + refresh JWT, and writes them as **HttpOnly**
   `Secure` `SameSite=Lax` cookies with `Domain=.product-data-api.com` configuration to allow sharing auth state across the frontend (`dashboard.product-data-api.com`) and the API backend (`api.product-data-api.com`). Bearer JWT is also accepted (API clients,
   tests).

Other endpoints: `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`,
`GET /api/v1/auth/me`.

### Per-provider verification

Reference services in Infera:
`apps/backend/src/main/java/com/infera/backend/service/{OidcTokenVerifierService,
GoogleTokenVerifierService,GithubTokenVerifierService,JwtTokenService}.java`.

| Provider | Mechanism | Notes |
|---|---|---|
| **Google** | OIDC ID token; verify signature against Google JWKS, check `iss` (`https://accounts.google.com`), `aud` = client id, `exp` | standard OIDC |
| **Microsoft** | OIDC ID token; JWKS from the tenant/`common` metadata; verify `iss`, `aud`, `exp` | multi-tenant issuer handling |
| **GitHub** | **NOT OIDC for id tokens** - GitHub issues an OAuth access token; exchange code -> token, then call the GitHub user API (`/user`, `/user/emails`) to get verified email + profile | requires a userinfo HTTP call, not JWKS |
| **Apple** | OIDC ID token verified against Apple JWKS (`iss=https://appleid.apple.com`, `aud`=client id); **client secret is a JWT you sign** with your Apple private key (ES256) for the token exchange | email may be a private relay; name only on first auth |

All client ids/secrets, JWKS URLs, issuers, and the Apple signing key are config
keys (below). Verification failures -> `401 invalid-credentials`.

## 3. Organization RBAC

Roles on `organization_members.role`. Permission matrix:

| Capability | OWNER | ADMIN | DEVELOPER | BILLING |
|---|:--:|:--:|:--:|:--:|
| Transfer ownership / destructive org actions | yes | - | - | - |
| Manage members & roles | yes | yes | - | - |
| Create/rotate/revoke any key | yes | yes | own only | - |
| Use playground / read usage | yes | yes | yes | - |
| Manage billing / payment / subscriptions | yes | yes | - | yes |
| Read balance / invoices / transactions | yes | yes | yes | yes |

Enforce with method security (`@PreAuthorize`) resolving the caller's role in the
**active organization** (from session/JWT org context). Disabled actions in the
UI must explain the missing role (see [`../b2b/b2b-ui.md`](../b2b/b2b-ui.md) 7.2).

**Platform admin** (`/api/v1/admin/**`) is separate from org roles: gated by
`users.is_platform_admin`, computed from the admin-email allowlist. Every admin
mutation writes an `admin_audit_events` row.

## 4. Configuration keys (`application.yml` / `-devsec.yml` / env)

```yaml
b2b:
  auth:
    jwt:
      secret: ${B2B_JWT_SECRET:}        # HS256 symmetric signing secret (minimum 256-bit strong key)
      algorithm: HS256
      access-ttl: 15m
      refresh-ttl: 30d
    cookie:
      domain: .product-data-api.com     # Wildcard domain for cross-origin sharing
      secure: true
      same-site: lax
    admin-emails: ${B2B_ADMIN_EMAILS:}  # comma-separated allowlist
    oidc:
      google:    { client-id: ${B2B_OIDC_GOOGLE_CLIENT_ID:},    client-secret: ${B2B_OIDC_GOOGLE_SECRET:} }
      microsoft: { client-id: ${B2B_OIDC_MS_CLIENT_ID:},        client-secret: ${B2B_OIDC_MS_SECRET:}, tenant: common }
      github:    { client-id: ${B2B_OIDC_GH_CLIENT_ID:},        client-secret: ${B2B_OIDC_GH_SECRET:} }
      apple:     { client-id: ${B2B_OIDC_APPLE_CLIENT_ID:},     team-id: ${B2B_OIDC_APPLE_TEAM_ID:}, key-id: ${B2B_OIDC_APPLE_KEY_ID:}, private-key: ${B2B_OIDC_APPLE_PRIVATE_KEY:} }
  cors:
    allowed-origins: ${B2B_ALLOWED_ORIGINS:https://product-data-api.com}
```

Secrets are never committed; `-devsec.yml` is excluded from the jar (as
`front-api` does). See [local runbook](../operations/product-data-api-local-runbook.md)
for test OIDC setup.
