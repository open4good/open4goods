# Product Data API - Stripe billing contract

> Canonical authority: [`../b2b/00-canonical-decisions.md`](../b2b/00-canonical-decisions.md).
> **No in-house reference exists** (Infera has no Stripe code), so this contract
> is authored from scratch. Grants map to credit buckets per
> [`product-data-api-billing-ledger.md`](product-data-api-billing-ledger.md).
> Credits are granted **only** after a verified, idempotent Stripe event.

## Catalog binding

The billing catalog (packs + subscriptions) is YAML
(`b2b-catalog.yml`, shape in `b2B.md`): each entry has `amount-eur`,
`credits`/`monthly-credits`, and a `stripe-price-id` bound from env. The frontend
pricing page renders this via `GET /api/v1/customer/billing/catalog` - never
hardcoded. Stripe price/product objects are created out-of-band (Stripe
dashboard) and referenced by id; the app does not create products at runtime.

```
B2B_STRIPE_SECRET_KEY, B2B_STRIPE_WEBHOOK_SECRET
B2B_STRIPE_PACK_{STARTER,GROWTH,SCALE}_PRICE_ID
B2B_STRIPE_SUB_{STARTER,GROWTH,SCALE}_PRICE_ID
```

## Checkout flows

- **Pack (prepaid, one-time)**: `POST /api/v1/customer/billing/checkout/pack`
  with `{ catalogId }` -> create a Checkout Session `mode=payment` for the pack's
  `stripe-price-id`, attach `organization_id` + `catalogId` as `metadata` and
  `client_reference_id`, persist a `stripe_checkout_sessions` row (`OPEN`).
  Return the session URL.
- **Subscription**: `POST /api/v1/customer/billing/checkout/subscription` with
  `{ catalogId }` -> Checkout Session `mode=subscription`, same metadata, persist
  row. Return URL.
- **Billing portal**: `POST /api/v1/customer/billing/portal` -> create a Stripe
  Billing Portal session for the org's `stripe_customer_id`; return URL (manage
  payment method, cancel subscription, view invoices).

A `stripe_customers` row (one per org) is created lazily on first checkout if
absent.

## Webhook endpoint

`POST /api/v1/billing/stripe/webhook` (public, raw body):

1. **Verify signature** with `B2B_STRIPE_WEBHOOK_SECRET` (`Webhook.constructEvent`).
   Invalid -> `400`, no side effects.
2. **Idempotency**: insert `stripe_events(stripe_event_id, type, payload)`. If the
   id already exists (unique violation), return `200` immediately - already
   processed. Do this **before** side effects, in the same transaction as the
   grant where possible.
3. Dispatch by `type` (below). Unknown types are recorded and ignored (`200`).
4. Always return `200` on successful processing so Stripe stops retrying;
   transient failures return `5xx` so Stripe retries.

### Events handled

| Event | Action |
|---|---|
| `checkout.session.completed` | Mark `stripe_checkout_sessions` `COMPLETED`. For `mode=payment` (pack): create a `PACK` bucket (`credits` from catalog, `expires_at=NULL`) + `GRANT` ledger row. For `mode=subscription`: record/ensure `stripe_subscriptions` row; the actual credits come from the first `invoice.paid`. If the `stripe_subscriptions` row was already lazily created by an earlier `invoice.paid` event due to out-of-order delivery, update its status and metadata appropriately. |
| `invoice.paid` | If the invoice belongs to a subscription: create a `SUBSCRIPTION` bucket (`monthly-credits` from catalog, `expires_at`=period end + grace) + `GRANT` row, then **enforce the rollover cap** (ledger spec). Upsert an `invoices` row (`paid`, `hosted_invoice_url`, `credits_granted`). If the target `stripe_subscriptions` record is not yet present (because `checkout.session.completed` is arriving late/delayed), lazily create a placeholder subscription record (with status `pending_activation`) so that processing the payment does not fail. |
| `customer.subscription.updated` | Sync `stripe_subscriptions` (`status`, `current_period_end`, `cancel_at`). If `cancel_at` set, set +30d `expires_at` on live SUBSCRIPTION buckets for that subscription. |
| `customer.subscription.deleted` | Mark subscription `canceled`; set +30d expiry on its live SUBSCRIPTION buckets (cancellation expiry, ledger spec). |
| `invoice.payment_failed` (optional) | Record on `invoices`/subscription status; surface in admin billing. No credit change. |

All credit changes go through the bucket/ledger service so every grant produces a
`credit_transactions` GRANT row and reconciles the Redis hot balance.

## Customer billing endpoints

- `GET /api/v1/customer/billing/catalog` - YAML packs + subscriptions (public-safe).
- `POST /api/v1/customer/billing/checkout/pack`
- `POST /api/v1/customer/billing/checkout/subscription`
- `POST /api/v1/customer/billing/portal`
- `GET /api/v1/customer/billing/balance` - live bucket sum + breakdown by kind.
- `GET /api/v1/customer/billing/transactions` - paged ledger.
- `GET /api/v1/customer/billing/invoices` - `invoices` rows.
- `GET /api/v1/customer/subscriptions` - current subscriptions.

Role gating per [auth spec](product-data-api-auth.md) (`BILLING`/`ADMIN`/`OWNER`
manage; all roles may read balance).

## Admin endpoints (manual grants & oversight)

- `POST /api/v1/admin/organizations/{organizationId}/credits/grants` - manual
  `MANUAL` bucket + `GRANT` row + `admin_audit_events` row. Body: credits,
  reason, optional expiration, internal note.
- `GET /api/v1/admin/organizations[/{id}[/transactions]]`, `GET /api/v1/admin/usage`,
  `GET /api/v1/admin/api-keys`, `POST /api/v1/admin/api-keys/{id}/revoke`.

## Local testing

Use the Stripe CLI: `stripe listen --forward-to
localhost:8087/api/v1/billing/stripe/webhook` (prints the webhook signing secret
-> `B2B_STRIPE_WEBHOOK_SECRET`), then `stripe trigger checkout.session.completed`
/ `invoice.paid` etc. Use Stripe **test mode** keys. See the
[runbook](../operations/product-data-api-local-runbook.md).

## Tests

Stripe webhook idempotency (duplicate event id does not double-grant); pack
purchase creates a non-expiring bucket; subscription `invoice.paid` creates a
SUBSCRIPTION bucket and enforces the 3-month rollover cap; cancellation sets +30d
expiry; signature failure rejected. Mock the Stripe client; assert on the
ledger/bucket side effects.
