# Product Data API - Redis key contract

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> **Redis is never authoritative.** Postgres (`credit_buckets` +
> `credit_transactions`) is the ledger of record
> ([`product-data-api-data-model.md`](product-data-api-data-model.md)). Redis
> holds only: an atomic hot balance mirror for reservation, an API-key lookup
> cache, a rate-limit counter, and a usage event stream. Any Redis key can be
> dropped at any time and rebuilt from Postgres.

## Key namespaces

All keys are prefixed `b2b:`. TTLs are tuned in `application.yml`; defaults below.

| Key | Type | Value | TTL | Rebuilt from |
|---|---|---|---|---|
| `b2b:org:{orgId}:balance` | string (int) | hot credit balance mirror | 24h (sliding) | `SUM(credits_remaining)` over live buckets |
| `b2b:org:{orgId}:lock` | string | reservation fence (set by Lua) | n/a | - |
| `b2b:apikey:{sha256}` | hash | `{orgId, keyId, status}` | 10 min | `api_keys` by `key_hash` |
| `b2b:ratelimit:{keyId}:{window}` | string (int) | request count in window | = window | ephemeral |
| `b2b:usage` | stream | one entry per finished request | maxlen ~1e6 (approx) | n/a (drained to `usage_events`) |
| `b2b:lastused:{keyId}` | string (ts) | debounced last-used write | 60s | n/a |

`{sha256}` is the hex SHA-256 of the full clear `pdapi_` key (same value stored
in `api_keys.key_hash`).

## Balance lifecycle

1. **Lazy load.** On first reservation for an org, if `b2b:org:{orgId}:balance`
   is absent, compute it from Postgres (sum of live bucket `credits_remaining`)
   and `SET` it. Guard the load with `SET NX` so concurrent requests do not
   double-load.
2. **Reserve.** The Lua `reserve` script atomically checks and decrements the hot
   balance by the facet's **maximum** possible cost (catalog value). Returns the
   new balance, or `-1` if insufficient -> caller returns **402**.
3. **Settle.** After Postgres durable settlement of the **actual** cost, refund
   the difference `(reserved - actual)` to the hot balance with `refund`, then
   reconcile the hot balance to the freshly computed Postgres sum
   (`reconcile`), so Redis cannot drift positive.
4. **No-data-no-pay.** When actual cost is 0 (invalid GTIN already rejected
   before reserve; or 404 / no-fresh-offer found after fetch), refund the full
   reservation. No Postgres debit is written.

Reservation prevents two concurrent requests from each seeing enough balance and
overspending. Because the **hot mirror** is reserved (not individual buckets),
bucket selection happens only in Postgres at settlement time; the hot balance is
a pessimistic ceiling reconciled immediately after.

## Lua scripts

Scripts are loaded once (`SCRIPT LOAD`) and called by SHA. `KEYS[1]` is always
the balance key.

### reserve(balanceKey, maxCost) -> newBalance | -1
```lua
-- ARGV[1] = maxCost (int)
local bal = redis.call('GET', KEYS[1])
if not bal then return -2 end           -- caller must lazy-load then retry
bal = tonumber(bal)
local cost = tonumber(ARGV[1])
if bal < cost then return -1 end        -- insufficient -> HTTP 402
return redis.call('DECRBY', KEYS[1], cost)
```
`-2` signals "balance not loaded": the caller loads from Postgres under `SET NX`
and retries once.

### refund(balanceKey, amount) -> newBalance
```lua
-- ARGV[1] = amount to add back (reserved - actual, or full reservation)
if not redis.call('GET', KEYS[1]) then return -2 end
return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
```

### reconcile(balanceKey, durableBalance) -> ok
```lua
-- ARGV[1] = authoritative balance from Postgres (sum of live buckets)
redis.call('SET', KEYS[1], tonumber(ARGV[1]))
redis.call('EXPIRE', KEYS[1], 86400)
return 1
```
Called right after settlement and by the periodic reconciliation job. Postgres
always wins.

### Rate limit (sliding-window counter)
```lua
-- KEYS[1] = b2b:ratelimit:{keyId}:{window}; ARGV[1]=limit, ARGV[2]=windowSeconds
local n = redis.call('INCR', KEYS[1])
if n == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2])) end
if n > tonumber(ARGV[1]) then return -1 end   -- over limit -> HTTP 429
return n
```

## Usage event stream

After each request, push one entry to `b2b:usage` (`XADD`) with fields:
`orgId, keyId, facetId, gtin, requestId, httpStatus, billable, creditsConsumed,
noPayReason, responseTimeMs, ts`. A consumer group (`XREADGROUP`, group
`hardener`) drains it into the Postgres `usage_events` table in
`HardenerBatch` ([billing ledger spec](product-data-api-billing-ledger.md)).
The stream is capped (`XADD ... MAXLEN ~ N`); losing tail entries degrades
analytics only, never billing (billing is settled synchronously in Postgres).

## Reconciliation & drift

- After **every** billable settlement, the hot balance is reconciled from
  Postgres (see `reconcile`), so steady-state drift is zero.
- A periodic `@Scheduled` reconciliation (configurable, e.g. every 5 min) also
  recomputes the hot balance for orgs with recent activity, covering crash
  windows between reserve and settle (a crashed request leaves the hot balance
  under-counted by the reservation; reconciliation restores it from Postgres).
- If Redis is unavailable, the API fails closed on metered endpoints (return
  `503`); it does **not** silently serve unmetered. Configurable.

## Configuration keys (`application.yml`)

```yaml
b2b:
  redis:
    balance-ttl: 24h
    apikey-cache-ttl: 10m
    usage-stream-maxlen: 1000000
    reconcile-interval: 5m
    fail-closed: true
  ratelimit:
    requests-per-minute: 600   # per key; see ops spec
```
