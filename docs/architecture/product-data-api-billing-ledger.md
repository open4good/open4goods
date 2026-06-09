# Product Data API - Credit ledger & bucket algorithm

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> Reads with [`product-data-api-data-model.md`](product-data-api-data-model.md)
> (tables) and [`product-data-api-redis-contract.md`](product-data-api-redis-contract.md)
> (hot path). This is the **billing-correctness contract** and the highest-risk
> part of v1. Implement it as a service-level workflow, never in controller code.

## Core principle

- **Reserve the maximum, settle the actual.** Redis reserves the catalog max cost
  before work; Postgres settles the real billable cost after work; the difference
  is refunded to Redis.
- **No data, no pay.** Actual cost is 0 unless the requested facet returned fresh,
  served, non-empty data. Invalid GTIN / 404 / no-fresh-offer => 0 credits, no
  Postgres debit.
- **Idempotent.** A billable request debits at most once, enforced by the partial
  unique index on `credit_transactions(request_id) WHERE type='DEBIT'`.
- **Authoritative balance** = `SUM(credits_remaining)` over live buckets
  (`credits_remaining > 0 AND (expires_at IS NULL OR expires_at > now())`).

## Request settlement flow

For a billable-capable request (`GET /api/v1/products/{gtin}/price`):

```
1.  Authenticate: Bearer pdapi_ -> SHA-256 -> b2b:apikey cache (Postgres fallback)
    -> orgId, keyId. Reject (401) if missing/malformed/unknown/revoked.
2.  Validate GTIN (BarcodeValidationService). Invalid -> 400, ZERO credits,
    no reservation. (no-pay-reason = 'invalid-gtin')
3.  maxCost = catalog.facet('product.price').credits   // e.g. 5
4.  reserved = redis.reserve(orgId, maxCost)
       reserved == -2 -> lazy-load balance from Postgres, retry once
       reserved == -1 -> 402 Payment Required, ZERO credits
5.  product = ProductRepository.getByIdWithoutEmbedding(gtin)
       not found -> actualCost = 0  (no-pay-reason = 'not-found', http 404)
6.  map sanitized price DTO; determine "served":
       served = at least one offer fresher than freshnessDays (default 30)
       not served -> actualCost = 0 (no-pay-reason = 'no-fresh-offer', http 200,
                     billable=false, empty price payload)
       served     -> actualCost = maxCost
7.  if actualCost == 0:
       redis.refund(orgId, reserved)        // give back full reservation
       (no Postgres debit row)
8.  else (actualCost > 0):
       settleDebit(orgId, requestId, facetId='product.price', gtin, actualCost)
          - SERIALIZABLE/locked transaction, expiring-first bucket debit (below)
          - insert append-only DEBIT transaction(s), idempotent on requestId
       refund = reserved - actualCost   // = 0 when maxCost == actualCost
       if refund > 0: redis.refund(orgId, refund)
       redis.reconcile(orgId, durableBalance)   // Postgres sum, prevents drift
9.  emit usage event to b2b:usage (orgId, keyId, facetId, gtin, requestId,
       httpStatus, billable, creditsConsumed, noPayReason, responseTimeMs)
10. response envelope meta + headers (see contract spec)
```

Steps 7-9 must also run on the error path (e.g. mapping throws after reserve):
release the reservation in a `finally`/`afterCompletion` so a crash never leaks
reserved credits beyond the next reconciliation tick.

## Expiring-first bucket debit (`settleDebit`)

Run inside a single Postgres transaction; lock the org's live buckets to
serialise concurrent debits.

```
settleDebit(orgId, requestId, facetId, gtin, cost):
  -- idempotency short-circuit
  if exists credit_transactions where type='DEBIT' and request_id=requestId:
      return  -- already billed (retry/duplicate)

  remaining = cost
  buckets = SELECT * FROM credit_buckets
            WHERE organization_id = orgId
              AND credits_remaining > 0
              AND (expires_at IS NULL OR expires_at > now())
            ORDER BY expires_at NULLS LAST, created_at      -- expiring first
            FOR UPDATE
  for b in buckets:
      if remaining == 0: break
      take = min(b.credits_remaining, remaining)
      b.credits_remaining -= take
      remaining -= take
      INSERT credit_transactions(type='DEBIT', organization_id=orgId,
            bucket_id=b.id, credits = -take, facet_id, gtin, request_id)
  if remaining > 0:
      -- balance changed between reserve and settle (rare; e.g. expiry).
      rollback; refund full reservation in Redis; return 402
  commit
```

Notes:
- One `DEBIT` row **per bucket touched**, all sharing `request_id`. The partial
  unique index is on `request_id` for `type='DEBIT'`, so the *first* insert with
  that `request_id` succeeds and a duplicate request transaction aborts -> caught
  as already-billed. (If a single request legitimately spans multiple buckets,
  insert them in one transaction; the uniqueness is enforced at commit and the
  duplicate-request guard is the initial `exists` check + transaction-level
  idempotency. If strict per-row uniqueness is required, use a composite
  `(request_id, bucket_id)` unique index instead - decide at implementation and
  keep the DDL and this doc in sync.)
- Authoritative balance after commit = recomputed sum; pushed to Redis via
  `reconcile`.

## Grants -> buckets

| Event | Bucket created |
|---|---|
| First login / org creation | `kind=FREE_GRANT`, `credits_total=2500`, `expires_at=NULL`; set `organizations.free_grant_applied=true` (one-time, guarded) |
| Stripe pack purchase (`checkout.session.completed`, mode=payment) | `kind=PACK`, credits from catalog, `expires_at=NULL`, `source_ref=session/invoice id` |
| Stripe subscription invoice (`invoice.paid` for a subscription) | `kind=SUBSCRIPTION`, `monthly-credits` from catalog, `expires_at` = period end + grace, `source_ref=subscription id`; then **enforce rollover cap** (below) |
| Manual admin grant | `kind=MANUAL`, admin-specified credits, optional `expires_at`, `note=reason`; + `admin_audit_events` row |

Every grant also writes a `GRANT` row to `credit_transactions` (positive
credits). Every grant invalidates/reconciles the Redis hot balance.

## Subscription rollover cap

On each subscription grant for a plan, after creating the new bucket:

```
cap = catalog.subscription(planId).monthlyCredits * rolloverCapMonths   // *3
liveSubCredits = SUM(credits_remaining) of SUBSCRIPTION buckets for this org/plan
while liveSubCredits > cap:
    expire oldest SUBSCRIPTION bucket(s) down to the cap:
       reduce/zero credits_remaining on the oldest bucket,
       INSERT credit_transactions(type='EXPIRE', credits = -expired)
    recompute liveSubCredits
```
This guarantees retained subscription credits never exceed
`monthlyCredits * rolloverCapMonths`.

## Cancellation expiry

On `customer.subscription.deleted` (or `updated` with `cancel_at` set): set
`stripe_subscriptions.cancel_at`, and set `expires_at = cancel_time + 30 days` on
all live SUBSCRIPTION buckets for that subscription. Expired credits are removed
by the expiry sweep below (with `EXPIRE` ledger rows). PACK/FREE_GRANT/MANUAL
buckets are unaffected.

## HardenerBatch (`@Scheduled`)

A single scheduled component owns the background reconciliation duties:

1. **Drain usage stream** `b2b:usage` (consumer group `hardener`) -> insert
   `usage_events` rows; `XACK`.
2. **Expiry sweep**: for buckets with `expires_at <= now()` and
   `credits_remaining > 0`, write an `EXPIRE` transaction for the remainder and
   zero `credits_remaining`.
3. **Balance reconciliation**: for orgs active since the last run, recompute the
   Postgres live-bucket sum and `reconcile` the Redis hot balance (covers
   crashed reserve/settle windows).
4. **Last-used flush**: persist debounced `b2b:lastused:{keyId}` into
   `api_keys.last_used_at`.

Intervals are configurable (`b2b.hardener.*`). The batch is idempotent: re-runs
never double-count (stream entries are `XACK`-ed; expiry is guarded by
`credits_remaining > 0`; reconciliation is a SET to the durable value).

## Test matrix (Testcontainers Postgres + Redis)

Mirror `b2B.md` acceptance: unauthenticated 401; bad/revoked key 401; invalid
GTIN 400 + 0 credits; missing product 404 + 0 credits; no-fresh-offer 200
billable=false + 0 credits; fresh offer 200 + exactly 5 credits + one durable
DEBIT row; insufficient balance 402; duplicate `request_id` debits once;
subscription grant rolls over and enforces the 3-month cap; cancellation sets
+30d expiry; manual admin grant writes GRANT + audit rows.
