---
title: "ADR 0011: Price observation time series"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [11]
---

# ADR 0011: Price observation time series

## Context

The legacy product keeps at most one minimum-price history per condition for two years.
It does not retain the provider dimension, and `DataFragment.priceHistory` is not an
ingestion history contract. This cannot support a reproducible B2B provider time series.
Persisting every poll would, however, create billions of duplicate observations.

## Decision

`services/price-history` owns three stores and exposes ports used by ingestion,
projection assembly and B2B queries:

1. A versioned `OfferHead` index keeps the latest state for
   `(gtin, providerId, providerOfferId)`: condition, currency, amount, availability,
   provider timestamps, `firstSeenAt`, `lastSeenAt`, content hash and usage policy.
2. An Elasticsearch time-series data stream keeps immutable `PriceChangeEvent` values.
   The dimensions are GTIN, public provider id, provider offer id, condition and
   currency. The timestamp is observation time; amount is a gauge. An event is appended
   on first sight, amount/currency/condition/availability change, disappearance or
   reappearance. An unchanged poll updates only `OfferHead.lastSeenAt`.
3. A daily provider-rollup data stream groups by GTIN, provider, condition and currency
   and stores minimum, maximum, closing amount, observed offer count, first/last
   observation and change count. It is the default long-range query source.

Raw change events are retained for 24 months and daily rollups for five years. Data
stream lifecycle, templates and mappings are versioned. Production serving data has one
replica; a zero-replica benchmark may inform cost reporting but is not the release
default. Capacity keeps at least 30 percent disk headroom and a tested snapshot restore.

Event ids are deterministic from dimensions, timestamp, event kind and payload hash;
retries are idempotent. Late observations inside the open rollup window recompute the
affected day. Older late events remain in raw history and enqueue an explicit rollup
repair. Currency values are ISO 4217 and are never converted implicitly.

The B2B history facet defaults to daily data for the last 30 days. `DAY` queries allow a
five-year interval; `CHANGE` queries allow at most 31 days and require data still present
in raw retention. Both use inclusive `from`, exclusive `to`, stable cursor pagination,
provider/condition/currency filters and policy filtering before response mapping. Eight
credits are settled only for a non-empty response; invalid, absent, empty or forbidden
series consume zero credits.

Legacy `Product` minima may be backfilled as provider `LEGACY_PRODUCT_MIN`, with
condition, currency, value and original timestamp only. No merchant, observation span or
availability is inferred. This series is excluded from B2B and open data by default and
cannot outrank provider-attributed observations.

## Consequences

Historical prices remain queryable by provider without inflating product documents or
writing one point per scrape. The source index supports narrow forensic queries; daily
rollups carry long-range B2B traffic and batch aggregation. Price storage and throughput
must be measured from observed offer-change rates before the full-catalog migration.
