# Product Alert MVP

## Goal

The MVP introduces price-drop alerts for products already aggregated by `api`.

Users are identified by email only. They can subscribe to a GTIN and a product
condition (`NEW` or `OCCASION`). When the aggregated best price drops, the
system creates notification candidates for matching subscriptions.

The MVP stops at candidate creation. It does not send emails yet.

## Functional Scope

### Public flow

1. A caller registers or re-registers an email with `POST /v1/users`.
2. A caller subscribes an email to a GTIN with `POST /v1/subscriptions`.
3. The subscription can target:
   - a condition (`NEW` or `OCCASION`)
   - any price decrease
   - optionally a maximum target price

### Internal flow

1. `crawler` sends `DataFragment` updates to `api`.
2. `api` aggregates product prices as it already does.
3. `api` compares the best price before and after aggregation for each GTIN and
   condition.
4. When the best price decreases, `api` calls `product-alert` over HTTP.
5. `product-alert` resolves matching subscriptions and stores
   `PENDING` notification candidates.

## Important Functional Clarification

Alerts are based on the aggregated best market price for a GTIN and a condition.

They are not merchant-specific alerts. If one merchant drops a price but the
aggregated best price does not move, no alert is produced.

## Architecture

### `api`

- The integration point is `DataFragmentStoreService.aggregateAndstore(...)`.
- Price-drop detection runs inside the ingestion flow only.
- Sanitisation and scoring batches do not emit alert events.
- Emission is best effort and must never block product indexation.

### `services/product-alert`

- Dedicated Spring Boot microservice.
- Stores users, subscriptions and notification candidates in Elasticsearch.
- Exposes:
  - public endpoints for user and subscription upsert
  - an internal endpoint for price-event ingestion

## Data Model

### Users

Index: `product-alert-users`

- `id`: normalized lowercase email
- `email`
- `status`: `ACTIVE`
- `createdAt`
- `updatedAt`

### Subscriptions

Index: `product-alert-subscriptions`

- `id`: deterministic `email#gtin#condition`
- `email`
- `gtin`
- `condition`
- `alertPrice`
- `alertOnDecrease`
- `enabled`
- `createdAt`
- `updatedAt`
- `lastTriggeredAt`
- `lastTriggeredPrice`

### Notification candidates

Index: `product-alert-notification-candidates`

- `id`: deterministic from subscription identity, price and event timestamp
- `subscriptionId`
- `email`
- `gtin`
- `condition`
- `previousPrice`
- `currentPrice`
- `eventTimestamp`
- `status`: `PENDING`
- `createdAt`

## API Contract

### Public endpoints

#### `POST /v1/users`

Upserts a user from an email.

#### `POST /v1/subscriptions`

Upserts an active subscription from:

- `email`
- `gtin`
- `condition`
- `alertPrice` optional
- `alertOnDecrease`

Subscriptions auto-create the user if needed.

### Internal endpoint

#### `POST /internal/v1/price-events`

Accepts a batch of price-drop events:

- `gtin`
- `condition`
- `previousPrice`
- `currentPrice`
- `eventTimestamp`

Returns counters:

- received events
- matched subscriptions
- created candidates

## Implementation Choices

### GTIN handling

The public API accepts GTIN as a string and normalizes it before persistence.
Internally, subscriptions and events are matched on the numeric GTIN value.

### Deduplication

Two protections are used:

1. deterministic notification candidate IDs for exact event retries
2. subscription-level deduplication with `lastTriggeredAt` and
   `lastTriggeredPrice` within a configurable window

### Ingestion robustness

- `api` groups buffered fragments by GTIN before computing the final price-drop
  events of the batch
- `api` serializes aggregation per GTIN in-process
- `api` publishes alert events after aggregation has completed for the GTIN

## Configuration

### `api`

`price-alerting.*`

- `enabled`
- `base-url`
- `connect-timeout`
- `read-timeout`
- `api-key`

### `product-alert`

`product-alert.*`

- `security.enabled`
- `internal.api-key`
- `dedup-window`

## Observability

### `api`

- counters for emitted price events, success and failure
- logs containing GTIN and condition

### `product-alert`

- counters for received events, matched subscriptions, created candidates and
  ingestion failures
- dedicated health indicator

## Tests

The implementation is covered by:

- unit tests for price-drop detection and aggregation-side publication
- unit tests for subscription matching and deduplication
- HTTP client integration tests for `api -> product-alert`
- Elasticsearch repository integration tests with Testcontainers

## Explicitly Out Of Scope

- email delivery
- digest scheduling
- double opt-in workflow
- unsubscribe flow
- external event bus
