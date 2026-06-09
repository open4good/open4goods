# Product Data API - Rate limiting & observability

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> A public, paid API needs abuse protection and operability from day one. This
> spec is informed by the recurring **front-api OOM** incident (Undertow worker
> pool x fat `@Cacheable` results = heap exhaustion under load) - the B2B API must
> not repeat that pattern.

## Rate limiting

- Per-API-key sliding-window counter in Redis (`b2b:ratelimit:{keyId}:{window}`,
  Lua `INCR`+`EXPIRE`, see [redis contract](product-data-api-redis-contract.md)).
- Default `b2b.ratelimit.requests-per-minute: 600` per key (configurable, and
  overridable per plan later). Over limit -> `429 rate-limited` + `Retry-After`.
- Rate limiting is independent of credit metering: a `429` consumes **0** credits
  and is checked before reservation.
- Optionally a coarse per-IP limit on `/api/v1/auth/**` to slow OIDC abuse.

## Metrics (Micrometer)

Expose via Actuator/Prometheus. Tag by `facet`, `httpStatus`, `billable`,
`organization` (bounded - drop or hash org tag if cardinality is a problem):

- request count & latency (timer, percentiles p50/p95/p99) per facet;
- billable vs non-billable ratio; `no_pay_reason` breakdown;
- credits consumed (counter);
- 4xx/5xx counts (`401`, `402`, `404`, `429`, `500`);
- Redis reservation outcomes (reserved / 402 / refund);
- Postgres settlement timer; HardenerBatch drain size & duration;
- Stripe webhook processed/duplicate/failed counts.

## Health & readiness

`/actuator/health` with component health for Postgres, Redis, and Elasticsearch
(`ProductRepository`). Readiness gates on Postgres + Redis (the API fails closed
on metered endpoints when Redis is down, per the redis contract).

## Logging

- Structured logs, every request line carries `requestId`, `orgId`, `keyId`
  (prefix only), `facet`, `httpStatus`, `billable`, `creditsConsumed`, `ms`.
- **Never log**: full API keys, JWTs, Stripe secrets, OIDC tokens, customer PII
  beyond org id. Mask keys to their `key_prefix`.
- Align with `docs/services_observability_and_monitoring.md` conventions.

## Heap & capacity guidance (avoid the front-api OOM)

- Bound every cache: the `b2b:apikey` cache is in Redis with TTL, not an
  unbounded in-process `@Cacheable`. If any in-process cache is added (Caffeine),
  set a hard `maximumSize` and short TTL - never cache full product/offer payloads
  unbounded.
- Do not hold large ES result sets in memory; the price endpoint fetches a single
  product (`getByIdWithoutEmbedding`, embedding excluded by design).
- Tune the web server worker pool relative to heap; document the chosen
  `server.*` / thread settings in the runbook. Load-test before launch.
- Set explicit JVM `-Xmx`; expose `jvm.memory.*` metrics and alert on sustained
  old-gen pressure.

## Configuration

```yaml
b2b:
  ratelimit:
    requests-per-minute: 600
management:
  endpoints.web.exposure.include: health,info,prometheus,metrics
  endpoint.health.show-details: when-authorized
```

## Alerts (post-v1, document intent now)

402 spike (a customer ran dry), 5xx spike, Redis/Postgres/ES down, HardenerBatch
lag growing (usage stream backing up), Stripe webhook failures, old-gen heap
pressure.
