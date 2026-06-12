# Review Generation Service

Hosts the product enrichment pipeline that generates French AI-assisted product
content from product metadata, discovered source URLs, fetched markdown sources,
attributes, and prompt templates. The Maven artifact remains
`reviewgeneration`; new APIs use `/enrichment/...` and legacy `/review/...`
routes are deprecated delegates.

## Functional Flow

1. Eligibility: skip products with a fresh and structurally valid French AI
   review unless generation is forced.
2. URL discovery: submit DataForSEO Standard SERP tasks for
   `BRAND "MODEL" ("fiche technique" OR caractéristiques OR test OR guide OR comparatif)`.
   Results are stored in `Product.sourceUrls`, deduped by canonical URL, and
   capped to 20 URLs per product.
3. Fetching: read `Product.sourceUrls` when available, otherwise fall back to
   the existing Google Custom Search path, then fetch selected URLs concurrently
   through `UrlFetchingService`.
   Review generation requests `HTTP`, then `PLAYWRIGHT`, then `PROXIFIED`
   through internal strategy override headers; without an override, the URL
   fetcher uses `urlfetcher.domains`.
4. Markdown extraction: remove configured header, footer, cookie, newsletter,
   and other noise lines before token counting and prompt injection.
5. Source gating: keep only sources within per-source token bounds and require
   minimum global tokens plus minimum source count.
6. Attribute extraction: call `review-generation-attributes` and persist the
   extracted attributes on the product.
7. Text completion: call `review-generation`, normalize citations, validate
   URLs, persist the French review, populate product attributes, and index the
   product.
8. Hooks: execute post-generation hooks, including Google indexation.

## DataForSEO URL Discovery

```yaml
dataforseo:
  serp:
    username: "${DATAFORSEO_USERNAME}"
    password: "${DATAFORSEO_PASSWORD}"
    base-url: "https://api.dataforseo.com"
    language-code: "fr"
    location-name: "France"
    se-domain: "google.fr"
    device: "desktop"
    priority: 1
    depth: 10
    max-stored-urls: 20
    max-tasks-per-post: 100
```

Discovery uses DataForSEO Standard asynchronous tasks. Vertical discovery
submits at most 100 tasks per `task_post` request and stores a local JSON
tracking file in the review-generation batch folder. Polling endpoints and the
scheduled poller read task results and persist `organic` plus
`featured_snippet` URLs to `Product.sourceUrls`.

`site:` operators are intentionally not part of the default DataForSEO query.
Official and support domains are classified after discovery using the existing
source classification logic.

## Legacy SERP Configuration

Preferred domains are configured by the consuming application, not by this
service module. In the API application they live in
`api/src/main/resources/application.yml`. They are used in the first preferred
query and in result ordering so they are never starved by `maxSearch` when
alternate models exist. `preferred-domains-by-vertical` may override the global
list for a vertical when appliance and electronics review sources differ.

```yaml
review:
  generation:
    preferred-domains: ${API_DEFINED_PRODUCT_REVIEW_DOMAINS}
    preferred-domains-by-vertical:
      tv:
        - lcd-compare.com
        - lesnumeriques.com
      refrigerator:
        - quechoisir.org
        - electromenager-compare.com
    query-template: "test %s \"%s\""
    max-search: 5
    partial-retry-max-search: 2
    search-results-per-query: 10
    search-language-restrict: "lang_fr"
    search-country-restrict: "countryFR"
    search-geo-location: "fr"
    search-host-language: "fr"
    search-safe: "off"
```

For a product `SONY XR55A80L`, the first query is shaped like:

```text
(site:configured-domain.example OR site:another-configured-domain.example) ("SONY XR55A80L" OR "SONY AKA")
```

Vertical configs may still add `injectSitesResults`; those are queried after
the global preferred-domain query and before broad fallback queries.

Official manufacturer pages are identified before prompt-source selection.
Official support pages and accepted HTML sources are stored in
`Product.sourceUrls`; compatibility adapters still expose
`Product.officialUrl`, `Product.officialSupportUrls`, and `Product.reviewFacts`.
Official PDFs and extracted manuals are stored in `Product.resources` with
manufacturer metadata.

When normal source retrieval still fails thresholds, a conservative limited
fallback can persist review facts from trusted structured EPREL/IceCat
attributes and official product PDFs. The resulting diagnostics use
`LIMITED_STRUCTURED`, `LIMITED_OFFICIAL_PDF`, or
`LIMITED_STRUCTURED_AND_PDF`; generated reviews must expose the limited evidence
in `dataQuality` and avoid unsupported independent-test, user-feedback, price,
or date claims.

If the first fetch pass persists official manufacturer evidence but does not
reach the source or token thresholds, the service can run up to
`partial-retry-max-search` targeted manual, guide, review, and test searches.
The successful fetch response includes `searchedQueries`, `acceptedUrls`, and
`rejectedUrls`, plus `enrichmentStatus` for post-fetch hooks such as EPREL
completion, so back-office callers can inspect SERP quality without reading
logs.

## Fetching Configuration

Review generation requests fallback attempts in this order: simple HTTP,
browser-like rendering, then external anti-bot provider. Runtime fallback
overrides are consumed by the URL fetching service and are not forwarded to the
target site. Without an override, the concrete default strategy comes from the
URL fetching service:

```yaml
urlfetcher:
  domains:
    configured-domain.example:
      strategy: PLAYWRIGHT
      userAgent: "Mozilla/5.0"
    proxified-domain.example:
      strategy: PROXIFIED
      proxy:
        host: "${FETCH_PROXY_HOST}"
        port: 8080
```

Configure `urlfetcher.domains` beside `review.generation.preferred-domains` in
the API YAML when a site needs Playwright, a proxy, shorter timeout, or custom
headers.

## Markdown Extraction

Noise removal is line-based and configurable:

```yaml
review:
  generation:
    markdown-line-removal-patterns:
      - "(?i)^\\s*(menu|navigation|newsletter|footer|header)\\s*$"
      - "(?i)^\\s*(abonnez-vous|inscrivez-vous|suivez-nous).*$"
      - "(?i)^\\s*(cookies?|gestion des cookies|politique de confidentialite).*$"
      - "(?i)^\\s*(copyright|\\u00a9|tous droits reserves).*$"
```

Logs now expose the number of removed lines, accepted source URLs, fetch
strategy, source token count, and accumulated token count.

## Decoupled API Stages

Back-office API endpoints expose the three stages independently for one GTIN or
for a bounded set of products in a vertical:

- `POST /enrichment/{id}/urls/discover?force=false`
- `POST /enrichment/vertical/{verticalId}/urls/discover?limit=100&force=false`
- `POST /enrichment/discovery/jobs/{jobId}/poll`
- `GET /enrichment/discovery/jobs/{jobId}`
- `POST /enrichment/{id}/fetch`
- `POST /enrichment/{id}/attributes`
- `POST /enrichment/{id}/text`
- `POST /enrichment/{id}/workflow`
- `POST /enrichment/vertical/{verticalId}/fetch?limit=5`
- `POST /enrichment/vertical/{verticalId}/attributes?limit=5`
- `POST /enrichment/vertical/{verticalId}/text?limit=5`
- `POST /enrichment/vertical/{verticalId}/workflow?limit=5`

Each stage persists its result by default. Attribute extraction requires
fetched `Product.sourceUrls` or legacy `Product.reviewFacts`, and text
completion requires fetched sources plus persisted product attributes.

Deprecated `/review/...` equivalents remain available for current callers.

Durable technical details are maintained in
[docs/architecture/review-generation-service.md](../../docs/architecture/review-generation-service.md).

## Build & Test

```bash
mvn --offline -pl services/review-generation -am test
```

See the [main open4goods project](../../README.md) for details.
This module is provided under the [AGPL v3 license](../../LICENSE).
