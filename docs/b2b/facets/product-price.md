# Facet spec - `product.price`

> Status: IN BUILD (v1 facet)
> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md) ·
> Lifecycle: [`README.md`](README.md) · Catalogue:
> [`facet-catalog.md`](../product/facet-catalog.md) · Common contract:
> [API contract](../../architecture/product-data-api-contract.md)
>
> This is the v1 facet and the **canonical example** future facet specs follow
> (see [`authoring-prompt.md`](authoring-prompt.md)).

## 1. Overview and value proposition

Fresh, multi-merchant price and offer data for a GTIN: best price, best
new/occasion offers, sanitized offer list with merchant label, provenance, and
per-offer freshness. The commodity loss-leader of the catalogue: ~33.9M GTINs
covered, sold on a **contract no competitor matches** - GTIN-first synchronous
GET (vs job-based PriceAPI/Bright Data) and strict per-facet no-data-no-pay
(PriceAPI bills "not found"; SerpApi bills empty results). See
[`competition.md`](../business/competition.md) sections 5-6.

## 2. Coverage and data quality (measured)

| Measure | Result | Date |
|---|---:|---|
| Total index (`products-moustik`) | 90,330,515 | 2026-06-15 |
| `offersCount > 0` (sellable base) | 42,048,951 (46.5 %) | 2026-06-15 |
| `offersCount >= 2` (true price comparison) | 4,271,391 (4.7 %) | 2026-06-15 |
| `offersCount >= 5` (market depth) | 211,496 (0.2 %) | 2026-06-15 |
| Fresh within 30 days, among `offersCount > 0` | 82.7 % | 2026-06-15 |

Source study: [`data-coverage.md`](../business/data-coverage.md) section 2.2.
The 82.7 % freshness figure means the `no-fresh-offer` no-pay case will be rare
on the offer-bearing mass - good for conversion, honest to advertise.

Reproducible queries (verify field names against the live mapping first -
`GET products-moustik/_mapping`; method:
[`data-coverage.md`](../business/data-coverage.md) section 4):

```bash
ES=http://<devsec-es-host>:9200
IDX=products-moustik

curl -s "$ES/$IDX/_count"
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gt":0}}}}'
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gte":2}}}}'
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gte":5}}}}'
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"bool":{"filter":[
        {"range":{"offersCount":{"gt":0}}},
        {"range":{"lastChange":{"gte":"now-30d/d"}}}]}}}'
```

Quality probes (re-run at P5.6 against a running `b2b-api`, devsec profile;
pick 3-5 GTINs with `offersCount >= 2` via a small `_search`):

```bash
# find probe GTINs
curl -s "$ES/$IDX/_search?size=5&_source=id,offersCount" \
  -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gte":2}}}}'

# probe the facet endpoint; assert: 200, billable=true, X-Credits-Consumed: 5,
# offers carry merchant label + freshnessAgeDays, and NO compensation/
# affiliationToken/datasourceName fields anywhere in the payload
curl -i -H "Authorization: Bearer $KEY" \
  "http://localhost:8087/api/v1/products/<gtin>/price?language=en"
```

Thresholds: ship-ready as measured. Honest public claims: "33M+ GTINs with
offers", "multi-merchant comparison on ~4M products"; never claim full-catalog
coverage (83M includes offer-less identities).

## 3. Endpoint and credits

- Endpoint: `GET /api/v1/products/{gtin}/price?language=en|fr`
- Credits: **5** (= 0.010 EUR at 0.002 EUR/credit) - Baseline tier
  ([`facet-catalog.md`](../product/facet-catalog.md) section 3), aligned with
  PriceAPI Starter's included-credit cost (0.01 EUR) while being strictly more
  generous on billing (no-data-no-pay).
- Billable when: `fresh-offer` - at least one sanitized offer newer than
  `b2b.price.freshness-days` (default 30).

Catalog entry (already in the master prompt defaults):

```yaml
b2b:
  facets:
    product.price:
      path: /api/v1/products/{gtin}/price
      credits: 5
      doc: products/price
      billable-when: fresh-offer
```

The `data` DTO (`B2bPriceDto`: gtin, name, brand/model, offersCount,
freshOffersCount, bestPrice/bestNewOffer/bestOccasionOffer, offersByCondition,
trends, history summaries) and the offer shape (`B2bOfferDto`) are fully
specified in the [API contract](../../architecture/product-data-api-contract.md)
- the contract spec is normative for fields, sources, and `@Schema` examples.
GTIN -> `Long` normalization (leading zeros) is defined there too and must be
verified against `BarcodeValidationService` during P5.

## 4. Sanitization (allow-list)

Exposed fields: exactly the `B2bPriceDto`/`B2bOfferDto` tables of the
[API contract](../../architecture/product-data-api-contract.md) - the mapper
builds records field-by-field from `Product`/`AggregatedPrices`, adapted from
`front-api` `ProductMappingService`.

Facet-critical redactions (the source `AggregatedPrice` carries all of these):

| Hidden | Why |
|---|---|
| `compensation` | private commercial metadata |
| `affiliationToken` | affiliate secret |
| raw `datasourceName` | internal id; expose `shortDataSourceName()` label only |
| `quantityInStock`, `shippingTime`, `shippingCost` | unreliably collected; v1 must not imply stock/shipping data |

## 5. No-data-no-pay matrix

| Condition | HTTP | `billable` | no-pay reason |
|---|---:|---|---|
| invalid GTIN (checksum/format) | 400 | - | `invalid-gtin` (rejected before reserve) |
| valid GTIN, product absent from index | 404 | - | `not-found` |
| product exists, zero offers or none fresher than 30d | 200 | false | `no-fresh-offer` (empty price payload) |
| at least one fresh sanitized offer | 200 | true | - (debit 5) |

`meta.coverage` semantics: the price facet applies to every product, so
`covered` is always `true` in v1; a product without fresh offers is
covered-but-not-served (see the contract spec).

## 6. Docs pages

| Page | en | fr |
|---|---|---|
| Facet reference | `/docs/products/price` | `/fr/docs/products/price` |
| Playground | `/docs/products/price/playground` | mirror |
| Java quickstart | `/docs/products/price/documentation/java` | mirror |
| Python quickstart | `/docs/products/price/documentation/python` | mirror |

Reference page outline: endpoint + auth header; GTIN rules (8/12/13/14,
checksum, normalization); response envelope walkthrough with a full example;
freshness and the billable rule; the no-data-no-pay matrix above; provenance
(merchant labels); error examples (Problem Details 400/401/402/404). Shared
billing/auth/error concepts link to `/docs/billing-and-credits`,
`/docs/authentication`, `/docs/errors` rather than being restated.

## 7. SEO plan

Target queries (intent: developers searching for a price-data API):

| Query (en) | Query (fr) | Capturing page |
|---|---|---|
| product price API, price comparison API | API prix produit, API comparateur de prix | `/` + `/docs/products/price` |
| GTIN price lookup API / EAN price API | API prix EAN / code-barres | `/docs/products/price` |
| PriceAPI alternative | alternative PriceAPI | `/pricing` (comparator deferred - see open questions) |
| price API pay per result | API prix paiement au resultat | `/docs/billing-and-credits` |

- Slugs as in section 6; localized titles/descriptions per route
  (`@nuxtjs/seo`), hreflang en/fr pairs, canonical URLs.
- Structured data: `TechArticle` on the reference and quickstart pages;
  `FAQPage` on `/faq`; `Organization` + `WebSite` on `/`.
- Sitemap: all section 6 pages, both locales. Internal links: landing "facet
  roadmap" section, `/pricing`, `/docs` index, getting-started.
- Copy guardrails: lead with "No data, no pay" and "fresh GTIN-first price
  data"; never imply stock/shipping/marketplace data, full global coverage, or
  v1 availability of future facets ([`ui-spec.md`](../frontend/ui-spec.md)
  section 10).

## 8. Examples

```bash
# billable success
curl -s -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price?language=en"

# no-data-no-pay: product exists, no fresh offer -> 200, billable=false,
# X-Credits-Consumed: 0
curl -i -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/<stale-gtin>/price"

# invalid GTIN -> 400 Problem Detail, zero credits
curl -i -H "Authorization: Bearer pdapi_..." \
  "https://api.product-data-api.com/api/v1/products/123/price"
```

Java and Python snippets live in the quickstart pages (section 6); keep them
copy-paste runnable (plain `HttpClient` / `requests`, no SDK dependency in v1).

## 9. Playground behavior

- Sample mode (anonymous): fixture GTIN `0885909950805` with a canned billable
  response and a canned `no-fresh-offer` response; billing explainer panel.
- Live mode (session): key picker, proxy call via
  `POST /api/v1/customer/playground/products/price`, display of the executed
  request (masked key), response body + headers, `billable`, credits consumed/
  remaining, and the no-pay reason for the 200-non-billable case. States:
  success, 400, 401, 402, 404, no-data-no-pay
  ([`ui-spec.md`](../frontend/ui-spec.md) section 7.5).

## 10. Launch checklist

- [ ] Coverage re-measured at launch (section 2 queries) and results updated
- [ ] Catalog entry live; pricing page renders 5 credits from the backend catalog
- [ ] Backend endpoint + full test matrix green (P5 gate, runbook curl matrix)
- [ ] Quality probes (section 2) pass against devsec ES
- [ ] OpenAPI complete; frontend client regenerated
- [ ] Docs pages en + fr published (no placeholder French)
- [ ] Playground sample + live modes working
- [ ] SEO plan executed (metadata, hreflang, sitemap, structured data, internal links)
- [ ] `meta.coverage` reports `product.price` on every response
- [ ] Usage/no-pay-reason metrics visible (ops dashboards)
- [ ] P5/P10 tasks closed in [`tasks.md`](../implementation/tasks.md)

## 11. Open questions

- Public "our EUR/call vs PriceAPI/SerpApi" comparator on `/pricing`: deferred
  marketing decision ([`00-canonical-decisions.md`](../00-canonical-decisions.md)
  section 3).
- `lastChange` as the freshness proxy at index level vs per-offer `timestamp`
  at serving time: the billable decision uses per-offer timestamps; the ES
  coverage query uses `lastChange`. Confirm the two stay consistent when
  measuring at P5.6.
