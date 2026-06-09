# Product Data API - local runbook

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> How to run `b2b-api` + `b2b-frontend` locally end to end. Related memory:
> the open4goods devsec launch pattern (point Spring at source resources; ES
> devsec is the prod cluster).

## Prerequisites

- Java 21, Maven, pnpm, Node (per `frontend` tooling).
- Docker (Postgres + Redis).
- Stripe CLI (test mode) for webhook testing.
- Access to the devsec Elasticsearch (prod cluster, `products-moustik`) for real
  product lookups; otherwise mock `ProductRepository`.

## 1. Backing services (Docker)

```bash
docker run -d --name pdapi-postgres -e POSTGRES_DB=b2b -e POSTGRES_USER=b2b \
  -e POSTGRES_PASSWORD=b2b -p 5432:5432 postgres:16
docker run -d --name pdapi-redis -p 6379:6379 redis:7
```

Flyway runs `V1__product_data_api_init.sql` on boot (see
[data model](../architecture/product-data-api-data-model.md)).

## 2. Run `b2b-api`

Build the reactor module:

```bash
mvn -pl b2b-api -am install
```

Run with the `devsec` profile. As with `api`/`front-api`, the fat jar **excludes**
`application-devsec.yml`, so for a jar run point Spring at the source resources
(IDE/source runs pick it up automatically):

```bash
java -jar b2b-api/target/b2b-api-*.jar \
  --spring.profiles.active=devsec \
  --spring.config.additional-location=optional:file:./b2b-api/src/main/resources/
```

`devsec` points Elasticsearch at the prod cluster (`136.243.46.60:9200`, index
`products-moustik`). Postgres/Redis come from `application.yml` (localhost above).
Default port **8087**.

Required env (test values, never commit real secrets) - see
[auth](../architecture/product-data-api-auth.md) and
[stripe](../architecture/product-data-api-stripe-contract.md) for the full list:

```bash
export B2B_JWT_SECRET=dev-only-change-me
export B2B_ADMIN_EMAILS=goulven.furet@gmail.com
export B2B_STRIPE_SECRET_KEY=sk_test_...
export B2B_STRIPE_WEBHOOK_SECRET=whsec_...   # from `stripe listen` (step 4)
# OIDC client ids/secrets per provider you want to test
```

## 3. Seed an org + key

For local testing without OIDC, seed via SQL or an admin/dev endpoint:
1. insert an `organizations` row + `organization_members` OWNER;
2. apply the free grant (`FREE_GRANT` bucket 2500 + `GRANT` ledger row);
3. create an API key (capture the clear `pdapi_...` once).

OIDC login (full path): start `b2b-frontend`, sign in via a configured provider;
first login auto-provisions the org + free grant.

## 4. Stripe webhooks (local)

```bash
stripe login
stripe listen --forward-to localhost:8087/api/v1/billing/stripe/webhook
# copy the printed whsec_... into B2B_STRIPE_WEBHOOK_SECRET, restart b2b-api
stripe trigger checkout.session.completed
stripe trigger invoice.paid
```

## 5. Verify the price endpoint (curl matrix)

Each case is fully determined by the contract + ledger specs:

```bash
KEY=pdapi_...    # the clear key from step 3
BASE=http://localhost:8087/api/v1

# 401 - no key
curl -i $BASE/products/0885909950805/price

# 400 invalid GTIN, 0 credits
curl -i -H "Authorization: Bearer $KEY" $BASE/products/123/price

# 404 product not found, 0 credits
curl -i -H "Authorization: Bearer $KEY" $BASE/products/0000000000000/price

# 200 no fresh offer -> billable=false, X-Credits-Consumed: 0
# 200 fresh offer    -> billable=true,  X-Credits-Consumed: 5
curl -i -H "Authorization: Bearer $KEY" "$BASE/products/<gtin-with-offers>/price?language=en"

# 402 - drain the balance, then retry -> Payment Required
```

Check `X-Credits-Consumed` / `X-Credits-Remaining` headers and the `meta` block.
After a billable call, confirm a `credit_transactions` DEBIT row in Postgres and
that the Redis hot balance matches the bucket sum.

## 6. Run `b2b-frontend`

```bash
pnpm --dir b2b-frontend install
pnpm --dir b2b-frontend dev      # / (en) and /fr/
pnpm --dir b2b-frontend lint && pnpm --dir b2b-frontend typecheck \
  && pnpm --dir b2b-frontend test && pnpm --dir b2b-frontend build
```

OpenAPI client regeneration: see
[`../b2b/b2b-frontend-build.md`](../b2b/b2b-frontend-build.md).

## 7. Docs / OpenAPI

- `http://localhost:8087/v3/api-docs` (raw OpenAPI JSON)
- `http://localhost:8087/swagger-ui` (Swagger UI)
- Redoc/Scalar UI (path per `OpenApiConfig`)

## Blockers to record (if validation cannot run)

If Postgres/Redis/ES/Stripe is unavailable, record the exact command, the failure
reason, and the smallest next step (per `b2B.md` validation policy).
