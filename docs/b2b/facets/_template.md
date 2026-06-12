# Facet spec - `product.<facet-id>` <!-- e.g. product.impact -->

> Status: DRAFT | MEASURED | IN BUILD | LIVE
> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md) ·
> Lifecycle: [`README.md`](README.md) · Catalogue:
> [`facet-catalog.md`](../product/facet-catalog.md) · Common contract:
> [API contract](../../architecture/product-data-api-contract.md)

## 1. Overview and value proposition

What the facet returns, who buys it, and why nudger's data is credible here
(commodity volume vs proprietary exclusivity). One short paragraph plus the
competitive angle (link [`competition.md`](../business/competition.md) if
relevant).

## 2. Coverage and data quality (measured)

| Measure | Query | Result | Date |
|---|---|---:|---|
| e.g. products with field X | see below | n | YYYY-MM-DD |

Reproducible queries (method reference:
[`data-coverage.md`](../business/data-coverage.md) section 4). Verify field
names against the live mapping (`GET products-moustik/_mapping`) before
running.

```bash
# ES coverage queries (curl against devsec ES) - one per claim above
```

Quality probes: how to spot-check that served payloads are correct and fresh
(sample GTINs, API calls against a running b2b-api, what to assert).

```bash
# API probes
```

Thresholds: minimum coverage/quality required to ship; what is honest to claim
publicly. If the source block is not ES-indexed, state how coverage is counted
(pipeline-side counter) instead.

## 3. Endpoint and credits

- Endpoint: `GET /api/v1/products/{gtin}/<facet-path>`
- Credits: `<n>` (tier rationale vs [`facet-catalog.md`](../product/facet-catalog.md) section 3)
- Billable when: `<condition>` (e.g. fresh-offer, non-empty-scores)

Catalog entry:

```yaml
b2b:
  facets:
    product.<facet-id>:
      path: /api/v1/products/{gtin}/<facet-path>
      credits: <n>
      doc: products/<facet-path>
      billable-when: <condition>
```

Envelope, headers, and Problem Details follow the common
[API contract](../../architecture/product-data-api-contract.md) - do not
redefine them here. Define only the facet `data` DTO: fields, types, source
model paths, nullability, `@Schema` example values.

## 4. Sanitization (allow-list)

| Exposed field | Source | Notes |
|---|---|---|

Redactions specific to this facet, plus confirmation that the global redaction
table (compensation, affiliationToken, internal datasource ids, crawler/cache
keys, internal scoring) is honored.

## 5. No-data-no-pay matrix

| Condition | HTTP | `billable` | no-pay reason |
|---|---:|---|---|
| invalid GTIN | 400 | - | `invalid-gtin` |
| product not found | 404 | - | `not-found` |
| `<facet-specific empty/stale case>` | 200 | false | `<reason>` |
| served data | 200 | true | - |

`meta.coverage` semantics for this facet (especially if curated-subset only).

## 6. Docs pages

| Page | en | fr |
|---|---|---|
| Facet reference | `/docs/products/<facet-path>` | `/fr/docs/products/<facet-path>` |
| Playground | `/docs/products/<facet-path>/playground` | mirror |
| Java quickstart | `/docs/products/<facet-path>/documentation/java` | mirror |
| Python quickstart | `/docs/products/<facet-path>/documentation/python` | mirror |

Content outline per page (what must be covered, which examples).

## 7. SEO plan

- Target queries/keywords (en + fr), search intent, and the page that captures
  each.
- Slugs, localized titles/descriptions, hreflang pairs.
- Structured data type per page (`TechArticle`, `FAQPage`...).
- Sitemap entries; internal links (from `/docs`, `/pricing`, landing facet
  roadmap section).
- What NOT to claim (copy guardrails - e.g. no stock/shipping implications,
  honest coverage wording).

## 8. Examples

```bash
# curl - success + one no-data-no-pay case
```

Java and Python snippets (or links to the quickstart pages once written).

## 9. Playground behavior

Sample-mode fixture (GTIN + canned response) and live-mode expectations
(metering display, facet-specific empty states).

## 10. Launch checklist

- [ ] Coverage measured and recorded above (section 2), thresholds met
- [ ] Catalog entry + credits reviewed against tiers
- [ ] Backend endpoint + tests (incl. no-data-no-pay matrix) green
- [ ] OpenAPI annotations complete; frontend client regenerated
- [ ] Docs pages en + fr published (no placeholder French)
- [ ] Playground supports the facet (sample + live)
- [ ] SEO plan executed (metadata, hreflang, sitemap, structured data, internal links)
- [ ] Pricing page shows the facet from the backend catalog
- [ ] `meta.coverage` declared for the facet
- [ ] Usage/no-pay-reason metrics visible (ops dashboards)
- [ ] `FACET-<id>` run closed in [`tasks.md`](../implementation/tasks.md)

## 11. Open questions

Anything deferred, with who/when decides.
