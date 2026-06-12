# Facet spec authoring prompt (for AI agents)

Use this prompt to generate the spec for a new Product Data API facet. The
output is one file `docs/b2b/facets/product-<facet-id>.md`.

---

You are an AI agent working in `/home/goulven/git/open4goods`. Write the facet
specification for **`product.<FACET_ID>`** of the Product Data API (B2B brick).

## Inputs to read first

1. [`docs/b2b/00-canonical-decisions.md`](../00-canonical-decisions.md) - locked decisions; they win over everything.
2. [`docs/b2b/facets/_template.md`](_template.md) - your output must follow this structure exactly.
3. [`docs/b2b/facets/product-price.md`](product-price.md) - the canonical example of a finished spec; match its depth and tone.
4. [`docs/b2b/product/facet-catalog.md`](../product/facet-catalog.md) - the facet's planned endpoint, credit tier, source model blocks, and differentiation.
5. [`docs/b2b/business/data-coverage.md`](../business/data-coverage.md) - measured coverage and the ES query method (section 4).
6. [`docs/architecture/product-data-api-contract.md`](../../architecture/product-data-api-contract.md) - envelope, redaction table, coverage semantics.
7. The relevant `Product` model blocks (`model/src/main/java/.../product/`) and the matching `front-api` DTOs (`front-api/.../dto/product/`) listed in the catalogue's mapping table.

## Mandatory: measure before you write

Never copy coverage numbers from older docs without re-measuring. Numbers in
your spec must come from queries **you ran**, recorded with the date.

1. **Verify field names** against the live mapping first:
   `curl -s "$ES/products-moustik/_mapping" | jq 'paths(scalars) ...'` or
   inspect the `Product` model + `@Field` annotations. Several blocks are
   stored but NOT indexed (`reviews`, `eprelDatas`, `ranking`) - for those,
   coverage cannot be counted by ES query; say so explicitly and specify a
   pipeline-side counter instead.
2. **Run the coverage queries** (`_count`, aggs) against the devsec
   Elasticsearch (`products-moustik`, see the
   [runbook](../../operations/product-data-api-local-runbook.md)). Follow the
   query patterns of `data-coverage.md` section 4. Record query + result +
   date in section 2 of your spec.
3. **Probe data quality**: pick 3-5 sample GTINs covered by the facet, fetch
   them (ES `_doc` or, if `b2b-api` runs, the future endpoint), and check the
   payload would be non-empty, correct, and sanitized. Document the probes so
   they are re-runnable at launch.

## Spec requirements

- Follow `_template.md` section by section; do not invent or drop sections.
- **Reference, do not duplicate**: envelope, headers, error catalog, metering
  flow, and the global redaction table live in the architecture specs - link
  them. Your spec owns only the facet-specific parts.
- Define the complete no-data-no-pay matrix for the facet, including the
  facet-specific `no_pay_reason` literal(s), and the `meta.coverage` semantics
  (critical for curated-subset facets).
- Sanitization is allow-list: enumerate every exposed field with its source
  model path; never expose compensation, affiliation tokens, internal
  datasource ids, crawler/cache keys, or internal scoring/debug fields.
- The SEO plan must name target queries (en + fr), slugs, structured-data
  types, and copy guardrails (no overclaiming coverage; no implying data we do
  not have, e.g. stock/shipping).
- Credits must sit inside the tier table of `facet-catalog.md` section 3; if
  you deviate, justify against competitive anchors in
  [`competition.md`](../business/competition.md).
- ASCII punctuation; English; keep it scannable.

## After writing the spec

1. Update [`facet-catalog.md`](../product/facet-catalog.md) if your measured
   coverage or chosen credits differ from the planned values.
2. Add a `FACET-<id>` run section to
   [`implementation/tasks.md`](../implementation/tasks.md) referencing the
   spec's launch checklist.
3. Run `./scripts/lint.sh` and fix findings.
4. Hand off: summary, measured numbers, open questions that need a human
   decision (do not silently decide deferred questions listed in
   `00-canonical-decisions.md` section 3).
