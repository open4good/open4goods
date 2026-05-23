# Amazon PA-API Completion

Amazon completion enriches open4goods products through the official Amazon
Product Advertising API v5 (PA-API). It is implemented in the API module as a
standard product completion service.

## Current Implementation

- Service: `AmazonCompletionService`
- Configuration: `ApiProperties.amazonConfig`
- REST endpoints:
  - `GET /completion/amazon`
  - `GET /completion/amazon/?verticalConfig={id}&max={limit}`
  - `GET /completion/amazon/gtin/?gtin={gtin}`
- Pipeline hook: `CompletionFacadeService.processAll(...)`
- Product cache: `Product.datasourceCodes["amazon.fr"]`
- Default cache duration: 30 days

The service is disabled by default. It becomes active only when
`amazonConfig.enabled=true` and PA-API credentials are present.

## Flow

1. If a product already has `externalIds.asin`, the service calls PA-API
   `GetItems`.
2. If no ASIN is known, it calls `SearchItems` with the product GTIN as
   keywords.
3. Returned ASINs are stored in `externalIds.asin`.
4. Amazon offer listings are mapped to `DataFragment` prices:
   - `New` becomes `ProductCondition.NEW`.
   - `Used`, `Collectible`, and `Refurbished` become
     `ProductCondition.OCCASION`.
5. Amazon images are added as product resources with Amazon hard tags.
6. Item info fields such as title, brand, model, dimensions, energy class, and
   warranty are mapped into datafragment names, referential attributes, or raw
   attributes.
7. The standard realtime aggregator applies the fragments to the product.
8. The product datasource code is updated even when no Amazon item is found, so
   misses are also cached.

When both new and used Amazon offers point to the same detail URL, the service
keeps the Amazon affiliated URL unchanged and appends a condition fragment to
the internal datafragment URL. This avoids losing one offer because
`DataFragment` equality is URL-based.

## PA-API Constraints

PA-API is the official Amazon affiliate product API. Amazon documents `GetItems`
as the operation for item lookup, and `Offers.Listings.Price` as the resource
that returns offer buying prices:

- <https://webservices.amazon.com/paapi5/documentation/get-items.html>
- <https://webservices.amazon.com/paapi5/documentation/use-cases/using-offer-information/determining-price-merchant-and-delivery-information.html>

Operational constraints to account for:

- Credentials require an Amazon Associates account with PA-API access.
- New accounts are commonly constrained by low request rates. The default config
  uses a 1100 ms delay between calls and a batch cap of 8640 products.
- The service should be treated as an affiliate API integration, not a scraping
  fallback.

## Configuration Shape

```yaml
amazon-config:
  enabled: true
  access-key: "..."
  secret-key: "..."
  partner-tag: "nudger-21"
  host: "webservices.amazon.fr"
  region: "eu-west-1"
  marketplace: "www.amazon.fr"
  search-index: "All"
  sleep-duration: 1100ms
  refresh-duration: 30d
  max-calls-per-batch: 8640
  datasource-name: "amazon.fr.yml"
```

## Known Gaps

- ASIN matching still trusts the first PA-API search results for a GTIN query.
  A stricter GTIN or external-id validation step should be added if PA-API
  exposes enough evidence in the selected marketplace.
- There is no distributed rate limiter. Multiple API instances could exceed the
  account throttle if Amazon completion runs concurrently.
- The service stores only the ASIN and aggregated product data. It does not keep
  raw PA-API responses.
