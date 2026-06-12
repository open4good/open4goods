# Facet specs - lifecycle and authoring

> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md).
> Catalogue overview and credit tiers: [`facet-catalog.md`](../product/facet-catalog.md).
> Envelope/DTO/metering rules common to all facets:
> [API contract](../../architecture/product-data-api-contract.md).

A **facet** is one billable data product (`product.price`, `product.impact`,
...): one endpoint, one catalog entry, one credit price, one doc page, one SEO
surface. v1 ships `product.price` only; shipping each later facet is a
**recurring "run" task** in the project's life, executed against this playbook.

## Files here

| File | Purpose |
|---|---|
| [`_template.md`](_template.md) | Section-by-section template every facet spec follows |
| [`authoring-prompt.md`](authoring-prompt.md) | Prompt for an AI agent to generate a new facet spec |
| [`product-price.md`](product-price.md) | The v1 facet and the **canonical example** of a finished spec |

One file per facet, named `product-<facet>.md` (e.g. `product-impact.md`).
Facet specs **reference** the architecture contracts instead of duplicating
them; they own what is facet-specific: value proposition, coverage and quality
measurement, credits rationale, sanitization deltas, docs/SEO/playground
surfaces, and the launch checklist.

## Lifecycle of a new facet (the "run")

1. **Spec.** Generate the facet spec with [`authoring-prompt.md`](authoring-prompt.md)
   (AI-authored, human-reviewed). Add a `FACET-<id>` section to
   [`tasks.md`](../implementation/tasks.md) to track the run.
2. **Measure.** Run the spec's coverage and quality queries (ES `_count`/aggs
   and/or API probes) against the live index. Record results and the
   measurement date in the spec. A facet with unknown coverage does not ship.
3. **Price.** Confirm the credit price against
   [`facet-catalog.md`](../product/facet-catalog.md) tiers and competitive
   anchors ([`competition.md`](../business/competition.md)).
4. **Backend.** Add the `b2b-catalog.yml` entry, the sanitized DTOs
   (allow-list mapping), the endpoint, OpenAPI annotations, and the test matrix
   (including the facet's no-data-no-pay reasons).
5. **Docs.** Add `content/en` + `content/fr` doc pages, examples
   (curl/Java/Python), and playground support. Regenerate the OpenAPI client.
6. **SEO.** Execute the spec's SEO plan: page slugs (en + `/fr/`), localized
   metadata, hreflang, sitemap entries, structured data, internal links from
   `/docs` and `/pricing`.
7. **Launch.** Walk the spec's launch checklist; update the pricing page and
   the `meta.coverage` declaration; announce. Post-launch, monitor the facet's
   usage/no-pay-reason metrics ([ops spec](../../architecture/product-data-api-ops.md)).

## Rules

- **No-data-no-pay is per facet and contractual**: every spec must define
  exactly which conditions serve-and-bill vs serve-empty vs reject.
- **Sanitization is allow-list**: every spec lists its exposed fields and its
  redactions, cross-checked against the redaction table in the
  [API contract](../../architecture/product-data-api-contract.md).
- **Coverage claims must be measured, dated, and reproducible** - the queries
  live in the spec; the method reference is
  [`data-coverage.md`](../business/data-coverage.md) section 4.
- Premium facets covering only the curated subset must declare honest
  `meta.coverage` semantics in their spec.
