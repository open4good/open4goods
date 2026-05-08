# Review Generation Service

Generates French AI-assisted product reviews from product metadata, SERP
results, fetched markdown sources, and prompt templates. The service supports
realtime generation and OpenAI batch generation.

## Functional Flow

1. Eligibility: skip products with a fresh and structurally valid French AI
   review unless generation is forced.
2. SERP validation: require product brand and model, build preferred-domain
   queries first, then vertical-injected site queries, then broad model queries.
3. Fetching: fetch selected URLs concurrently through `UrlFetchingService`.
   The URL fetcher decides the real strategy from `urlfetcher.domains`
   (`HTTP`, `PLAYWRIGHT`, `PROXIFIED`).
4. Markdown extraction: remove configured header, footer, cookie, newsletter,
   and other noise lines before token counting and prompt injection.
5. Source gating: keep only sources within per-source token bounds and require
   minimum global tokens plus minimum source count.
6. LLM generation: call the configured prompt (`review-generation` by default,
   or `review-generation-grounded` for model-native web search), normalize
   citations, validate URLs, populate product attributes, and index the product.
7. Hooks: execute post-generation hooks, including Google indexation.

## SERP Configuration

Preferred domains are configured by the consuming application, not by this
service module. In the API application they live in
`api/src/main/resources/application.yml`. They are used in the first query so
they are never starved by `maxSearch` when alternate models exist.

```yaml
review:
  generation:
    preferred-domains: ${API_DEFINED_PRODUCT_REVIEW_DOMAINS}
    query-template: "test %s \"%s\""
    max-search: 5
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

## Fetching Configuration

Review generation requests fallback attempts in this order: simple HTTP,
browser-like rendering, then external anti-bot provider. The concrete strategy
comes from the URL fetching service:

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

## LLM Split Plan

The current prompt directly returns `AiReview`. To decouple the LLM phase,
introduce two prompt templates and a small intermediate DTO:

1. `review-generation-attributes`: consumes product metadata plus sources and
   returns extracted facts, normalized attributes, source references, and data
   quality. Its schema should be narrow and source-heavy.
2. `review-generation-text`: consumes product metadata plus extracted facts and
   returns the final `AiReview`. It should not read raw markdown, which reduces
   token usage and hallucination pressure.

Batch generation should run both phases in sequence per product before writing
tracking metadata. Realtime generation can run the same sequence synchronously,
streaming only the second call if UI progress needs text chunks.

## Build & Test

```bash
mvn --offline -pl services/review-generation -am test
```

See the [main open4goods project](../../README.md) for details.
This module is provided under the [AGPL v3 license](../../LICENSE).
