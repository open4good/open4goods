# Review Generation Service

Generates French AI-assisted product reviews from product metadata, SERP
results, fetched markdown sources, and prompt templates. The service supports
realtime generation and OpenAI batch generation.

## Functional Flow

1. Eligibility: skip products with a fresh and structurally valid French AI
   review unless generation is forced.
2. SERP validation: require product brand and model, build official-discovery
   and preferred-domain queries first, then vertical-injected site queries, then
   broad model queries.
3. Fetching: fetch selected URLs concurrently through `UrlFetchingService`.
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

## SERP Configuration

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

If the first fetch pass persists official manufacturer evidence but does not
reach the source or token thresholds, the service can run up to
`partial-retry-max-search` targeted manual, guide, review, and test searches.
The successful fetch response includes `searchedQueries`, `acceptedUrls`, and
`rejectedUrls` so back-office callers can inspect SERP quality without reading
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

- `POST /review/{id}/fetch`
- `POST /review/{id}/attributes`
- `POST /review/{id}/text`
- `POST /review/{id}/workflow`
- `POST /review/vertical/{verticalId}/fetch?limit=5`
- `POST /review/vertical/{verticalId}/attributes?limit=5`
- `POST /review/vertical/{verticalId}/text?limit=5`
- `POST /review/vertical/{verticalId}/workflow?limit=5`

Each stage persists its result by default. Attribute extraction requires
existing `Product.reviewFacts`, and text completion requires both
`Product.reviewFacts` and persisted product attributes.

Durable technical details are maintained in
[docs/architecture/review-generation-service.md](../../docs/architecture/review-generation-service.md).

## Build & Test

```bash
mvn --offline -pl services/review-generation -am test
```

See the [main open4goods project](../../README.md) for details.
This module is provided under the [AGPL v3 license](../../LICENSE).
