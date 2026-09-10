---
title: "ADR 0010: Source-neutral product reference"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [10, 12]
---

# ADR 0010: Source-neutral product reference

## Context

The legacy `Product` document is both source storage and published read model. Its
attribute path remaps every provider name through Icecat, loses part of the language
and unit evidence, applies global source priorities, and cannot replay a corrected
rule without fetching providers again. It also mixes reference facts, offers,
completion checkpoints, scores and presentation fields under one mutable object.

## Decision

`services/data-reference` owns the source record, canonical registry, normalization,
resolution and product read projection contracts. Import scheduling and provider HTTP
or file orchestration remain in `api`; provider modules map their responses to the
neutral input contract and never mutate a product projection.

A source record is identified by `(sourceId, sourceRecordId)`, independently of the
GTIN to which it is attached. Its replaceable head contains:

- schema version, provider version, observed/retrieved/expiry instants and content hash;
- `FULL` or `PARTIAL` completeness and `ACTIVE`, `DELETED`, `UNAVAILABLE` or `REJECTED` state;
- evidence reference, usage-policy reference and explicit GTIN links with confidence;
- ordered immutable assertions carrying a generic provider field identity and a sealed
  scalar, localized text, media, classification or relation evidence payload.

Brand, model, names and descriptions are scalar or localized assertions; images and
documents are media assertions; classes, model families and related GTINs are relation
assertions. Offers, prices and ingestion checkpoints are separate domains. Generic
records contain no Icecat, EPREL, Amazon or merchant field.

The current head is sufficient to replay current truth. An append-only journal keeps
head transitions, hashes, timestamps and outcomes, but not superseded provider values
or complete payloads. A repeated hash is idempotent. A `FULL` head replaces all prior
assertions; a `PARTIAL` head changes named coordinates only and uses explicit
tombstones. Older observations are journalled as ignored and never replace the head.

The authoritative O4G registry is versioned in Git and projected to an aliased runtime
index. Stable ids use `o4g:class:<slug>` and `o4g:attribute:<slug>`. Each attribute
declares value type, cardinality, translations, external mappings and, for a quantity,
one dimension and canonical UCUM code. Runtime writes cannot redefine the registry.

Normalization produces typed values: localized text, boolean, integer, decimal,
quantity, code, date and URI. Quantities use `BigDecimal`; provider unit aliases are
versioned mappings to unmodified UCUM codes. BCP 47 language tags use `und` for
non-linguistic or unknown content. Locale-sensitive parsing and formatting use
ICU4J/CLDR; formatting never changes stored values.

Usage policies are deny-by-default and keyed by source, content type and effective
period. They declare allowed `NUDGER_WEB`, `B2B_API` and `ODBL_EXPORT` surfaces,
retention, media caching, attribution, redistribution and legal-review metadata. The
resolver removes disallowed evidence before selection or derivation, including logs
and audit payloads exposed outside operations.

Resolution rules are versioned per canonical concept. Validated O4G corrections win in
their declared scope. EPREL is authoritative only for configured regulatory concepts
after exact model attachment. Icecat and merchant priority is concept-specific. The
remaining tie-break is confidence, observation time, then stable source and assertion
ids. Values are never averaged across sources unless a named concept rule says so.

One aliased projection index contains one document per normalized GTIN. The document
deduplicates candidate assertion ids by canonical field and stores a precomputed winner,
typed value, rule version, reason, publishable provenance and conflict flag per surface.
The projection assembler is the sole writer: it composes reference results with current
offer summaries, evaluation outputs and search fields supplied through typed ports.
Batch jobs query this read model or purpose-built rollups, never aggregate raw assertions.

GTIN remains the leaf identity. Projection fields `modelGroupId`, `familyGroupIds` and
`modelSearchTokens` make groups queryable without member arrays in group documents.
V1 creates automatic model groups only from explicit source relations or the exact tuple
of canonical brand, class and normalized full model. Family/prefix rules are reviewed,
versioned by brand and class; an unmatched prefix produces candidates, not membership.

## Consequences

Rule and policy changes can rebuild current projections without provider access. Storage
grows through source heads and surface decisions, so mappings and shard topology are
benchmarked against the complete read alias before ingestion. Cutover is a shadow build,
comparison, final delta and atomic alias switch. A legacy unattributed baseline may keep
otherwise unrecoverable GTIN identity fields, but cannot invent provider provenance or
enter B2B/open-data surfaces. The old product model is removed only after observation and
rollback windows close.
