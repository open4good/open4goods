# Facet specs - lifecycle and authoring

> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md).
> Catalogue overview and credit tiers: [`facet-catalog.md`](../product/facet-catalog.md).
> Envelope/DTO/metering rules common to all facets:
> [API contract](../../architecture/product-data-api-contract.md).

A **facet** represents a billable unit in the B2B API catalogue, which can fall into one of two categories:

1. **Product Facets**: Keyed by GTIN, returning product-specific data (`product.price`, `product.impact`, etc.) via `GET /api/v1/products/{gtin}/<facet-path>`.
2. **Utility Facets**: Function-oriented endpoints performing actions or transformations (e.g., `barcode.render`) that do not require a GTIN, typically queried via separate endpoints (e.g., `POST /api/v1/barcodes/render`).

Each facet is defined by one endpoint category, one catalog entry, one credit price structure, one documentation page, and one SEO surface. v1 ships `product.price` only; shipping each later facet is a **recurring "run" task** in the project's life, executed against this playbook.

## Files here

| File | Purpose |
|---|---|
| [`_template.md`](_template.md) | Section-by-section template every facet spec follows |
| [`authoring-prompt.md`](authoring-prompt.md) | Prompt for an AI agent to generate a new facet spec |
| [`product-price.md`](product-price.md) | The v1 product facet and the **canonical example** of a finished spec |
| [`barcode-render.md`](barcode-render.md) | The first utility facet specification |

File naming conventions:
- Product facets: `product-<facet>.md` (e.g., `product-impact.md`).
- Utility facets: named after their service domain/action (e.g., `barcode-render.md`).

Facet specs **reference** the architecture contracts instead of duplicating them; they own what is facet-specific: value proposition, coverage and quality measurement, credits rationale, sanitization deltas, docs/SEO/playground surfaces, and the launch checklist.

## Lifecycle of a new facet (the "run")

The lifecycle of a new facet is governed by a **business-first approach**. While technical implementation is usually straightforward following the facet template, the priority is to deep-dive into the market to ensure we outperform the competition, capture uncovered opportunities, and design a larger feature surface. 

To achieve this, the specification is authored through an **interactive state machine** between the AI agent and the human reviewer:

1. **State 1: Research & Competitive Analysis (Web Search & Approval)**
   - The agent uses web search to identify competitor APIs in the facet's domain, evaluating their feature surfaces and pricing structures.
   - The agent maps these features to identify uncovered gaps and propose a positioning that outperforms competitors.
   - **Checkpoint**: The agent presents the competitive findings, proposed pricing tier, and target SEO positioning to the user, waiting for explicit approval before proceeding.

2. **State 2: Coverage Measurement & Data Probing (Measurement & Approval)**
   - The agent verifies the field mappings against the live database index (Elasticsearch).
   - The agent runs coverage count/aggregation queries and performs quality probes on sample payloads.
   - **Checkpoint**: The agent presents the measured coverage stats, quality probe results, and proposed shipping thresholds to the user, waiting for approval before drafting.

3. **State 3: Spec Drafting (Template Application & Spec Approval)**
   - The agent writes the specification file `product-<facet-id>.md` following the [`_template.md`](_template.md).
   - **Checkpoint**: The agent presents the draft spec to the user for final feedback and approval.

4. **State 4: Registry Update & Implementation Checklist**
   - Add a `FACET-<id>` section to [`tasks.md`](../implementation/tasks.md) to track the run.
   - Add the `b2b-catalog.yml` entry.
   - Continue with the technical build steps:
     - **Backend**: Add DTOs, endpoint, OpenAPI annotations, and test matrix (no-data-no-pay).
     - **Docs & Playground**: Write en/fr documentation pages and configure playground mode.
     - **SEO Execution**: Set page slugs, metadata, hreflang, sitemaps, and internal linking.
     - **Launch**: Walk the launch checklist, publish the pricing update, and monitor metrics.

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
