# Analytics events (Plausible)

This document tracks the analytics event catalog for the Nuxt frontend and how
to instrument new UI interactions. Use the `useAnalytics` composable, which
wraps the Plausible plugin and respects Do Not Track settings.

## Where analytics live

- **Composable**: `app/composables/useAnalytics.ts`
- **Plugin guard**: `app/plugins/plausible-guard.client.ts`
- **Nuxt config**: `nuxt.config.ts` (plausible module configuration)

## Event catalog

### `affiliate-click`

Tracks clicks on affiliate redirect links (typically `/contrib/<token>`).

Props:

- `token` (string)
- `affiliatePlatform` (string, defaults to `unknown` until exposed by backend)
- `merchantId` (string/number, optional)
- `merchantName` (string, optional)
- `merchantSlug` (string, optional)
- `placement` (string, optional)
- `productId` (string/number, optional)
- `gtin` (string/number, optional)
- `vertical` (string, optional)
- `categorySlug` (string, optional)
- `offerRank` (number, optional)
- `priceBucket` (string, optional)
- `currency` (string, optional)
- `condition` (string, optional)
- `destinationHost` (string, optional)

Do not send the full destination URL in this event. Use `destinationHost` to
keep Plausible properties low-cardinality and avoid leaking query parameters.

### `product-redirect`

Legacy affiliate redirect event (kept for compatibility).

Props:

- `token`, `placement`, `source`, `url`

### `tab-click`

Tracks UI tab selection.

Props:

- `tab` (string)
- `context` (string)
- `label` (string, optional)
- `productId` (string/number, optional)

### `category-filter-change`

Tracks filter changes on category pages.

Props:

- `categoryId`, `categorySlug`
- `action` (e.g., `manual-updated`, `subset-enabled`, `cleared`)
- `source` (e.g., `sidebar`, `drawer`, `fast-filters`, `active-filters`)
- `filtersCount`
- `filterFields` (array of mapping strings)
- `subsetIds` (array of subset ids)

### `category-sort-usage`

Tracks category sort controls.

Props:

- `categoryId`, `categorySlug`
- `action` (`exposed`, `selected`, `order-updated`)
- `selectedField`, `selectedGroup`, `sortOrder`
- `defaultField`, `primaryOptions`, `advancedOptions`, `totalOptions`

### `search`

Tracks search submissions.

Props:

- `query`, `source`, `results`

### `search-focus`

Tracks focus on search inputs to measure intent.

Props:

- `location` (page or component identifier)
- `queryLength`

### `open-data-download`

Tracks open data download actions.

Props:

- `dataset`, `method`, `href`

### `file-download`

Tracks generic file downloads (e.g., PDF documentation).

Props:

- `fileType`, `url`, `label`, `context`

### `section-view`

Tracks section visibility (intersection observed).

Props:

- `sectionId`, `page`, `label`

### `share-resolution-*`

Tracks the web-share product resolution flow.

Events:

- `share-resolution-start`
- `share-resolution-select`
- `share-resolution-resolved`
- `share-resolution-timeout`
- `share-resolution-error`

Props vary by event and include `domainLanguage`, `originUrl`, `mode`,
`productId`, `source`, and `candidates`.

### `ai-review-methodology-click`

Tracks clicks from AI review request UI to the methodology page.

Props:

- `location`
- `labelVariant` (dialog only)

### `ai-review-request-variant-view`

Tracks the displayed AI review request CTA variant.

Props:

- `location`
- `labelVariant`

## Plausible dashboard setup

Configure these custom properties on the Plausible site to make the affiliation
goal useful in the self-hosted UI:

- `affiliatePlatform`
- `merchantSlug`
- `merchantName`
- `placement`
- `gtin`
- `vertical`
- `categorySlug`
- `offerRank`
- `priceBucket`
- `currency`
- `condition`
- `destinationHost`

Use `affiliate-click` as the canonical affiliation conversion goal. Keep
`product-redirect` only for historical comparisons while dashboards migrate.

## Implementation tips

1. **Use the composable**: `const { trackEvent, trackSearch, ... } = useAnalytics()`.
2. **Prefer semantic events**: If an interaction already maps to a helper
   (`trackAffiliateClick`, `trackFilterChange`, etc.), use it instead of
   `trackEvent` directly.
3. **Avoid personal data**: keep props to identifiers and UI context.
4. **Keep props stable**: analytics dashboards rely on consistent event names.
