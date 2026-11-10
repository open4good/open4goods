# Nudger Front API

This module provides the REST endpoints used by the Nuxt 3 frontend. It exposes data aggregated by other open4goods modules and is secured with JWT tokens.
The former `/api/v1/search` endpoint was removed from this module.
The main REST endpoints are provided by the `ProductController` class.
Use the main `api` service for search operations.

### Content endpoints

- `GET /blocs/{blocId}` - return a small HTML bloc from XWiki.
- `GET /pages/{xwikiPageId}` - return a rendered XWiki page with metadata.

## Default Port

The application runs on **port 8082** by default (`server.port` in `application.yml`). This keeps it separate from the main API on port 8081.

## Security

Security is enabled by default using JWT authentication. For local testing it can be disabled by setting
`front.security.enabled=false` in `application.yml` or via an environment variable.
To allow the Nuxt frontend running on a different host during development, configure the list of allowed
CORS origins using the `front.security.cors-allowed-hosts` property. You can override the default value
via the `FRONT_SECURITY_CORS_ALLOWED_HOSTS` environment variable. By default it permits requests from
`http://localhost:8082`.

Additional properties configure token generation:

- `front.security.jwt-secret` - secret key used to sign JWT tokens.
- `front.security.access-token-expiry` - duration before an access token expires (default `PT30M`).
- `front.security.refresh-token-expiry` - duration before a refresh token expires (default `P7D`).

With this configuration all calls stay on the same origin and the built-in CORS
rules apply correctly.

## Rate limiting

Requests are limited using an in-memory token bucket. Defaults can be changed in
`application.yml`:

- `front.rate-limit.anonymous` - requests per minute allowed for unauthenticated users.
- `front.rate-limit.authenticated` - requests per minute allowed for authenticated users.

When limits are exceeded the API responds with HTTP `429 Too Many Requests`.

## Search sort tie-break

An explicitly sorted `/products/search` page ends on
`ProductRepository.stableSortTieBreak()`: `gtinInfos.normalizedGtin14`
ascending (the GTIN zero-padded to 14 digits, a keyword with doc values),
missing values last, so ties such as equal ImpactScores keep their position
between requests and pages. Never sort on `id`: it is mapped without doc values
and Elasticsearch rejects the whole search, which made every sorted listing
return a 500 (GOU-1940). `ProductRepositorySortTieBreakIT` checks both against
the production mapping.

The category page server-renders its listing with the default sort
(ImpactScore descending) and reuses it after hydration only while that sort is
still applied. A failed search shows an error with a retry action instead of
the previous listing.

## Global search relevance

`SearchService.globalSearch` (`POST /products/search/global`, page
`/rechercher`) applies relevance rules held by `GlobalSearchRelevance` and
configured under `front.global-search` (GOU-2029). They never reach
`SearchService.search` (category pages, `/products/search`):
`SearchServiceTest` pins the query it sends.

- **Ranking** of both levels (category groups and long tail): text score, plus
  a `match_phrase` bonus on `offerNames` (`phrase-boost`, `phrase-slop`), plus
  `offers-count-weight` x `log1p(offersCount)` (`function_score`, `boost_mode`
  `sum`), so that at comparable relevance the product sold by more merchants
  comes first. Freshness stays a filter (`lastChange`), not a score.
- **Accessories**: a product is an accessory when one of `accessory-terms`
  (or its plural with `s`) starts its offer name, within
  `accessory-head-words` words. Elasticsearch checks it with an `intervals`
  query filtered on `interval.start`: the field analyzer folds accents and
  case, and a term further in the name ("lave-linge avec filtre
  anti-calcaire") does not count. Positions of the multi-valued `offerNames`
  run on from one value to the next, so only the first indexed offer name is
  checked. When the normalized query contains one of the terms, nothing
  changes. Otherwise accessories are demoted in the category groups
  (`boosting`, `accessory-negative-boost`) and leave the long tail for the
  additive `accessoryResults` field (at most `accessory-results-limit`).
- **Long tail** (`missingVerticalResults`, products without a vertical): only
  products with a fresh offer (`OfferFreshnessService`), an image and a
  readable title. The index holds neither `coverImagePath` nor an indexed
  image URL for these products, so the check runs in Java: the service reads
  the first `(page + 1) x pageSize x long-tail-over-read` candidates with a
  light source, keeps the displayable ones in order, serves the requested
  slice and fetches only that slice in full. An image is a cover or a merchant
  image that is not a placeholder (`noimage`). A readable title is an offer
  name of at least `min-title-words` words, one of them alphabetic, at least
  `min-title-word-length` letters long and other than the brand.
- **Post-filter limits**: `missingVerticalPage.totalElements` is exact when
  every candidate was read; otherwise it is the Elasticsearch total scaled by
  the share of displayable candidates read, so the last pages announced may be
  shorter or empty. At most `long-tail-max-candidates` candidates are read:
  `totalPages` is capped at `long-tail-max-candidates / (pageSize x
  long-tail-over-read)` (20 pages of 20).

## Offer freshness

front-api never serves an offer older than `front.offers.max-age` (default: the
api rule `ProductRepository.VALID_UNTIL_DURATION`, 2 days), measured from the
offer `timeStamp`; an offer without timestamp is treated as stale. The api drops
such offers only when it re-aggregates a product, and the daily batch only
re-aggregates vertical products: products outside verticals (books, DVDs...)
keep their old offers in Elasticsearch (GOU-1385).

`OfferFreshnessService` is the single filter. It applies to the offer list of
the product page, the best prices (overall, new, occasion: recomputed, the
precomputed `minPrice` is ignored), the offer count and occasion flag, the
`/contrib/` links (built only for fresh offers), the SEO meta description price
range, and the search suggestion and in-memory global search price sorts. The
frontend JSON-LD `Offer`/`AggregateOffer` is built from the offer list and
count, so it follows. A product whose offers are all stale is served as a
product without offer. Price histories and trends are kept.

`offers.newLastOfferSeen` / `offers.occasionLastOfferSeen` date the last offer
of each condition, stale ones included (GOU-1699): the latest of
`price.lastOfferSeen`, recorded by the api before it drops expired offers, and
the indexed offer timestamps. The frontend dates the "last known price" of a
product without offer with it; the price history only records price changes.
The `PRICE_LAST_SEEN_*` timeline events use the same date once the offers are
dropped.

Elasticsearch filters and aggregations on `offersCount` and `price.minPrice`
still read the indexed values.

- `front.offers.max-age` - maximum offer age (Spring `Duration`, e.g. `3d`;
  env `FRONT_OFFERS_MAX_AGE`).

## Merchant domains count

`GET /stats/categories` returns `activeMerchantDomains` = `{ count, computedAt,
franceOnly }` (GOU-2037): the number of distinct merchant domains with at least
one active offer. It backs the "compare prices from N merchants" claim and the
Google CSS 50-domain threshold, so it is conservative. Unlike
`affiliationPartnersCount` (every configured partner), it leaves out affiliation
networks, merchants without an active offer and duplicate domains.

- **Source**: `price.offers` is not indexed in Elasticsearch, so the api records
  the date of the last offer of each datasource as fragments are aggregated
  (`DatasourceOfferActivityService`, `GET /merchants/offer-activity`, see
  `api/AGENTS.md`). Nothing scans the product index.
- **Active**: the datasource's last offer is fresh under `front.offers.max-age`
  (same rule as `OfferFreshnessService`).
- **Merchant domain**, in this order: the
  `front.merchant-domains.datasource-domains` entry of the datasource, the portal
  URL of its affiliation partner, its name when the name is a domain
  (`darty.com`). Offer URLs are never used: they are affiliation links
  (`awin1.com`...). A datasource without domain is not counted (logged).
- **Normalisation**: host only, lower case, without `www.`, reduced to the
  registrable domain of the public suffix list (Guava `InternetDomainName`):
  `https://fr.aliexpress.com` and `www.aliexpress.com` both count as
  `aliexpress.com`.
- **Not a merchant**: domains of `front.merchant-domains.excluded-domains`
  (Awin, TradeTracker, Effiliation, Kwanko, CJ, Webgains...) and their
  subdomains.
- **France**: a partner whose `countryCodes` do not include `FR` is left out.
  `franceOnly` is true only when every counted domain states its countries; the
  feeds give none today, so it is `false` (delivery to France is not filtered).
- **When**: `MerchantDomainsService.refresh()`, 30 s after startup then hourly,
  never in a user request. `computedAt` dates it; on failure the previous count
  is kept. `activeMerchantDomains` is `null` until the first success. Each
  refresh logs the counted domains, and the datasources not counted.
- **Warm-up**: the api starts recording at its first deployment with this
  feature (`trackedSince`); until `front.offers.max-age` has elapsed, merchants
  whose last offer predates it are missing and a warning is logged.

## Google CSS dry run

`front.css.*` (`CssProperties`, disabled by default) prepares the product pages sent to Google by the CSS API
(GOU-2079, lot GOU-2080). **Nothing is sent to Google**: no CSS client is instantiated, the module only depends on
`com.google.shopping:google-shopping-css` for the `CssProductInput` type.

- Eligibility (`CssEligibilityService`, one `CssExclusionReason` per failed rule): at least
  `front.css.min-merchant-domains` (2) distinct merchant domains with a fresh offer, an image, a real title (not the
  GTIN, not a generic title), a valid GTIN, a canonical URL. Offers are read through `OfferFreshnessService` and the
  domains through `MerchantDomainsService.domainResolver()` (registrable domain, affiliation networks excluded).
- Mapping (`CssProductInputMapper`): `rawProvidedId` = GTIN, `contentLanguage`/`feedLabel` from the properties,
  `cppLink` = `ProductCanonicalUrl.fullSlug` on `front.css.site-base-url`, `lowPrice`/`highPrice`/`numberOfOffers`/headline
  offer from the same fresh prices as the product page (amounts in micros, decimal arithmetic),
  `includedDestinations`, short `expirationDate`. `rating`, `reviewCount` and the ImpactScore are never sent.
  `productHighlights` is not filled: the index holds no such data.
- Dry run (`front.css.enabled=true`): `POST /admin/css/dry-run` (site editor role, like `/admin/cache/reset`) starts
  a background run (409 if one is running); `GET /admin/css/dry-run` returns the last report. Files written in
  `front.css.output-directory`: one `<vertical>.ndjson` per vertical (one `CssProductInput` per line) and
  `report.json` / `report.md` (per vertical: examined, eligible, count by exclusion reason; a product failing several
  rules counts once per reason).

## Contribution votes ip/ua purge

On every startup, once the application is ready, front-api runs on a background
thread the purge of the historical `ip` and `ua` fields of the
`contribution-votes` Elasticsearch index (board approval 2f4935d1, GOU-1230).
It removes only these two keys from the documents that hold one of them; no
other field changes and no document is deleted. It is idempotent: once the
index is clean, a run logs a single "rien à purger" line.

- `front.contribution-votes-purge.enabled` - toggle (default `true`,
  `FRONT_CONTRIBUTION_VOTES_PURGE_ENABLED`; `false` in the `local` profile).

It never blocks nor fails the startup: an error (Elasticsearch unreachable,
mapping where `exists` cannot see the fields) ends in one `ERROR` line and the
next startup retries. Every line starts with `contribution-votes purge ip/ua`
and holds counts only (documents before/after, with `ip` or `ua`, updated,
version conflicts, failures), never an IP or a User-Agent:

```bash
grep 'contribution-votes purge ip/ua' front-api.log
```

## Affiliation clicks mapping update

On every startup, once the application is ready, front-api writes on a
background thread the `AffiliationClick` mapping onto the existing
`affiliation-clicks` index (GOU-1828: `contentPath` as `keyword`). It is
additive and idempotent; no document is rewritten. An error ends in one
`ERROR` line starting with `affiliation-clicks mapping` and the next startup
retries; neither the startup nor the redirects are affected. See
[docs/architecture/affiliation-clicks.md](../docs/architecture/affiliation-clicks.md).

- `front.affiliation-clicks-mapping.enabled` - toggle (default `true`,
  `FRONT_AFFILIATION_CLICKS_MAPPING_ENABLED`; `false` in the `local` profile).

## Google Indexation

The front API can notify the Google Indexing API after AI review generation
succeeds. The integration is disabled by default and must be enabled explicitly.

Front API dispatch configuration:

- `front.google-indexation.enabled` - master toggle (default `false`).
- `front.google-indexation.realtime-enabled` - dispatch immediately on success.
- `front.google-indexation.batch-enabled` - dispatch queued URLs on schedule.
- `front.google-indexation.batch-interval` - schedule delay (default `30m`).
- `front.google-indexation.batch-size` - maximum URLs per batch.
- `front.google-indexation.max-attempts` - retry limit for failed URLs.
- `front.google-indexation.site-base-url` - public base URL used to build product URLs.

Google Indexing API credentials:

- `google-indexation.service-account-json` - raw service account JSON.
- `google-indexation.service-account-path` - file path to the service account JSON.

Credentials are required when `front.google-indexation.enabled=true`. You can
provide them via environment variables:

```bash
export FRONT_GOOGLE_INDEXATION_ENABLED=true
export FRONT_GOOGLE_INDEXATION_SITE_BASE_URL=https://nudger.fr
export GOOGLE_INDEXATION_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
```

## API documentation

Access to the Swagger UI (`/swagger-ui.html`) and the raw OpenAPI specification
(`/v3/api-docs`) requires valid XWiki credentials. Use HTTP Basic
authentication when requesting these endpoints. Example commands:

```bash
curl -u XWIKI_USER:XWIKI_PASS http://localhost:8082/swagger-ui.html
curl -u XWIKI_USER:XWIKI_PASS http://localhost:8082/v3/api-docs
```

To call secured REST endpoints you must obtain a JWT by authenticating with the
same credentials:

```bash
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"XWIKI_USER","password":"XWIKI_PASS"}'
```

The response returns `accessToken` and `refreshToken`; include the access token
in subsequent requests using the `Authorization: Bearer <token>` header.

## Domain localisation contract

Every HTTP endpoint now requires a `domainLanguage` query parameter. The value
is constrained to the `DomainLanguage` enum (`fr`, `en`) and is echoed back via
the `X-Locale` response header so the frontend can confirm which locale was
resolved. While localisation behaviour is not implemented yet, controllers and
DTOs are annotated to document which fields will eventually depend on this
parameter.

## Building

From this directory run:

```bash
mvn clean install
```

Or build from the repository root:

```bash
mvn -pl front-api -am clean install
```

## Testing

Execute the tests with:

```bash
mvn test
```

## Observability

Each request is logged with endpoint, status, client IP and user subject. Metrics counters are exposed via Spring Boot Actuator at /actuator/metrics.

## Migration notes

DTO classes such as `ProductDto` are now implemented using Java records. This
means all fields are immutable and accessed via record accessors
(e.g. `dto.base()`). Ensure any custom code or tests no longer rely on
setters.

## Blog service loading

Blog posts are fetched from XWiki by the `BlogService`. Posts are now loaded asynchronously when the application starts, so the API becomes available immediately. See [../docs/blog_service_async_loading_plan.md](../docs/blog_service_async_loading_plan.md) for details.
