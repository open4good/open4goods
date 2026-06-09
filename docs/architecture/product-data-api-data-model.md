# Product Data API - Postgres data model & DDL

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> Postgres is the **authoritative** store for organizations, users, API keys,
> credit buckets, and the append-only transaction ledger. Redis is a hot mirror
> only (see [`product-data-api-redis-contract.md`](product-data-api-redis-contract.md)).
> This document defines the 13 tables named in `b2B.md` and is the source for the
> canonical Flyway migration
> `b2b-api/src/main/resources/db/migration/V1__product_data_api_init.sql`.

The peaceful-peacock flat `account` / `credit_balance` schema is **superseded**
by this model.

## Conventions

- All ids are `UUID` (`uuid` column, app-generated with `UUID.randomUUID()` or
  `gen_random_uuid()` via `pgcrypto`). Surrogate keys, never expose internal
  sequence numbers to clients.
- Timestamps are `timestamptz`, UTC, default `now()`.
- Money is **never** floating point: credits are `bigint`; EUR amounts are
  `integer` cents.
- Enums are stored as `text` with a `CHECK` constraint (avoids Postgres enum
  migration friction; the Java side uses `@Enumerated(EnumType.STRING)` mapped to
  the same literals).
- Every FK is `ON DELETE RESTRICT` unless stated; we never hard-delete an
  organization with history (soft `status`).
- `request_id` strings are `pdreq_<base32>`; key prefixes are `pdapi_<8 chars>`.

## Entity overview

```
organizations 1───* organization_members *───1 users
      │ 1                                          │
      ├──* api_keys (created_by -> users)          │
      ├──* credit_buckets ──* credit_transactions ─┘ (actor_user_id)
      ├──1 stripe_customers ──* stripe_checkout_sessions
      │                      └─* stripe_subscriptions ──* invoices
      ├──* usage_events
      └──* admin_audit_events (target org; actor = admin user)
stripe_events (global, webhook idempotency)
```

---

## Tables

### organizations
The billable tenant. Owns credits, keys, billing, members, usage.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `name` | text NOT NULL | display name |
| `slug` | text UNIQUE NOT NULL | url-safe |
| `billing_email` | text | |
| `default_language` | text NOT NULL DEFAULT 'en' | CHECK in ('en','fr') |
| `status` | text NOT NULL DEFAULT 'ACTIVE' | CHECK in ('ACTIVE','SUSPENDED','CLOSED') |
| `free_grant_applied` | boolean NOT NULL DEFAULT false | guards the one-time 2500-credit grant |
| `created_at` / `updated_at` | timestamptz NOT NULL DEFAULT now() | |

Index: `(status)`.

### users
A human principal, provisioned from OIDC. A user may belong to many orgs.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `email` | text UNIQUE NOT NULL | lowercased |
| `display_name` | text | |
| `avatar_url` | text | from OIDC |
| `oidc_provider` | text NOT NULL | CHECK in ('GOOGLE','MICROSOFT','GITHUB','APPLE') (last used) |
| `oidc_subject` | text NOT NULL | provider `sub` |
| `is_platform_admin` | boolean NOT NULL DEFAULT false | computed from admin allowlist at login; cached here |
| `last_login_at` | timestamptz | |
| `created_at` / `updated_at` | timestamptz NOT NULL DEFAULT now() | |

Unique: `(oidc_provider, oidc_subject)`. Index: `(email)`.

### organization_members
Membership + role (RBAC). See [auth spec](product-data-api-auth.md) for the
permission matrix.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `user_id` | uuid FK -> users | |
| `role` | text NOT NULL | CHECK in ('OWNER','ADMIN','DEVELOPER','BILLING') |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Unique: `(organization_id, user_id)`. Partial unique: at most one `OWNER` per org
(`CREATE UNIQUE INDEX ... ON organization_members(organization_id) WHERE role='OWNER'`).

### api_keys
Opaque `pdapi_` keys. Store only prefix + SHA-256 hash; the clear key is shown
once at create/rotate. See [auth spec](product-data-api-auth.md).

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `created_by` | uuid FK -> users | |
| `name` | text NOT NULL | user label |
| `key_prefix` | text NOT NULL | e.g. `pdapi_a1b2c3d4`, shown in lists |
| `key_hash` | char(64) UNIQUE NOT NULL | SHA-256 hex of full clear key |
| `status` | text NOT NULL DEFAULT 'ACTIVE' | CHECK in ('ACTIVE','REVOKED','ROTATED') |
| `last_used_at` | timestamptz | updated async |
| `rotated_from` | uuid FK -> api_keys NULL | lineage |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |
| `revoked_at` | timestamptz NULL | |

Index: `(organization_id, status)`, `(key_prefix)`.

### credit_buckets
**Authoritative balance** = sum of `credits_remaining` over non-expired,
non-zero buckets. Debit ordering is expiring-first; the algorithm lives in
[`product-data-api-billing-ledger.md`](product-data-api-billing-ledger.md).

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `kind` | text NOT NULL | CHECK in ('FREE_GRANT','PACK','SUBSCRIPTION','MANUAL') |
| `credits_total` | bigint NOT NULL CHECK (>=0) | as granted |
| `credits_remaining` | bigint NOT NULL CHECK (>=0) | mutated on debit/expire |
| `expires_at` | timestamptz NULL | NULL = never expires (FREE_GRANT, PACK, MANUAL by default) |
| `source_ref` | text NULL | stripe subscription/invoice id, or admin grant ref |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Indices for debit ordering: `(organization_id, credits_remaining)` and
`(organization_id, expires_at NULLS LAST, created_at)`.
Debit query selects `credits_remaining > 0 AND (expires_at IS NULL OR expires_at > now())`
ordered by `expires_at NULLS LAST, created_at` (oldest-expiring first).

### credit_transactions
Append-only ledger. **Never updated or deleted.** Source of audit and invoices.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `bucket_id` | uuid FK -> credit_buckets NULL | which bucket moved (NULL for multi-bucket summary rows; one row per bucket touched) |
| `type` | text NOT NULL | CHECK in ('GRANT','DEBIT','REFUND','EXPIRE','ADJUST') |
| `credits` | bigint NOT NULL | signed: GRANT/REFUND > 0, DEBIT/EXPIRE < 0, ADJUST either |
| `facet_id` | text NULL | e.g. `product.price` (DEBIT/REFUND) |
| `gtin` | text NULL | the requested product (DEBIT/REFUND) |
| `request_id` | text NULL | idempotency key for a billable request |
| `actor_user_id` | uuid FK -> users NULL | admin for ADJUST/manual GRANT |
| `note` | text NULL | manual grant reason / audit |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

**Idempotency**: `CREATE UNIQUE INDEX ux_credit_tx_debit_request ON
credit_transactions(request_id) WHERE type='DEBIT'` - guarantees a billable
request debits at most once even on retry. Index: `(organization_id, created_at desc)`.

### stripe_customers
One Stripe customer per org.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid UNIQUE FK -> organizations | |
| `stripe_customer_id` | text UNIQUE NOT NULL | `cus_...` |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

### stripe_checkout_sessions
Tracks pack/subscription checkouts to correlate webhooks.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `stripe_session_id` | text UNIQUE NOT NULL | `cs_...` |
| `mode` | text NOT NULL | CHECK in ('payment','subscription') |
| `catalog_id` | text NOT NULL | YAML pack/subscription id (e.g. `starter`) |
| `status` | text NOT NULL DEFAULT 'OPEN' | CHECK in ('OPEN','COMPLETED','EXPIRED') |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

### stripe_subscriptions
Active/past subscriptions; drives monthly grant buckets.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `stripe_subscription_id` | text UNIQUE NOT NULL | `sub_...` |
| `catalog_id` | text NOT NULL | YAML subscription id |
| `status` | text NOT NULL | mirrors Stripe: 'active','past_due','canceled', etc. |
| `current_period_end` | timestamptz | |
| `cancel_at` | timestamptz NULL | set on cancellation; +30d bucket expiry derives from here |
| `created_at` / `updated_at` | timestamptz NOT NULL DEFAULT now() | |

### invoices
Customer-visible invoice/payment history (mirror of Stripe invoices).

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `stripe_invoice_id` | text UNIQUE NOT NULL | `in_...` |
| `amount_cents` | integer NOT NULL | |
| `currency` | text NOT NULL DEFAULT 'eur' | |
| `status` | text NOT NULL | 'paid','open','void','uncollectible' |
| `hosted_invoice_url` | text NULL | Stripe download link |
| `credits_granted` | bigint NULL | bucket created from this invoice |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

### stripe_events
Webhook idempotency log. **Every** processed Stripe event id is recorded before
side effects so duplicates are no-ops.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `stripe_event_id` | text UNIQUE NOT NULL | `evt_...` |
| `type` | text NOT NULL | e.g. `checkout.session.completed` |
| `processed_at` | timestamptz NOT NULL DEFAULT now() | |
| `payload` | jsonb NULL | raw event for debugging |

### usage_events
Per-request analytics feed (drained from the Redis usage stream by
`HardenerBatch`). Powers dashboard/admin usage analytics.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `organization_id` | uuid FK -> organizations | |
| `api_key_id` | uuid FK -> api_keys NULL | |
| `facet_id` | text NOT NULL | `product.price` |
| `gtin` | text NULL | |
| `request_id` | text NOT NULL | |
| `http_status` | smallint NOT NULL | 200/400/401/402/404/429/500 |
| `billable` | boolean NOT NULL | |
| `credits_consumed` | bigint NOT NULL DEFAULT 0 | |
| `no_pay_reason` | text NULL | 'invalid-gtin','not-found','no-fresh-offer', NULL when billed |
| `response_time_ms` | integer | |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Indices: `(organization_id, created_at desc)`, `(facet_id, created_at)`,
`(http_status)`. Consider monthly partitioning when volume grows (out of v1).

### admin_audit_events
Append-only audit of admin actions (manual grants, key revocations, status
changes).

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `actor_user_id` | uuid FK -> users | the platform admin |
| `action` | text NOT NULL | 'CREDIT_GRANT','API_KEY_REVOKE','ORG_SUSPEND',... |
| `target_organization_id` | uuid FK -> organizations NULL | |
| `target_ref` | text NULL | e.g. api_key id, transaction id |
| `detail` | jsonb NULL | structured payload |
| `created_at` | timestamptz NOT NULL DEFAULT now() | |

Index: `(target_organization_id, created_at desc)`.

---

## Flyway migration

The canonical DDL goes in
`b2b-api/src/main/resources/db/migration/V1__product_data_api_init.sql`,
translating the tables above 1:1 (`CREATE EXTENSION IF NOT EXISTS pgcrypto;`
first for `gen_random_uuid()`). Keep CHECK constraints and the partial unique
indices (one OWNER per org; one DEBIT per `request_id`) - they are part of the
billing-correctness contract, not optional polish. Subsequent schema changes are
new `V2__...`, `V3__...` migrations; never edit `V1`.

## JPA mapping notes

- Entities under `org.open4goods.b2bapi.persistence.entity`, repositories under
  `...persistence.repository` (Spring Data JPA).
- Enum columns: `@Enumerated(EnumType.STRING)` with Java enums whose names match
  the CHECK literals exactly.
- `credit_transactions` and `admin_audit_events` are insert-only - expose no
  update/delete repository methods.
- Bucket debit uses a pessimistic-locked, ordered query
  (`@Lock(PESSIMISTIC_WRITE)` + the expiring-first ordering above) inside the
  settlement transaction; see the ledger spec.
