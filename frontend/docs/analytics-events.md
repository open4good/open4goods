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
- `url` (string, optional)
- `partner` (string, optional)
- `placement` (string, optional)
- `productId` (string/number, optional)

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

## Implementation tips

1. **Use the composable**: `const { trackEvent, trackSearch, ... } = useAnalytics()`.
2. **Prefer semantic events**: If an interaction already maps to a helper
   (`trackAffiliateClick`, `trackFilterChange`, etc.), use it instead of
   `trackEvent` directly.
3. **Avoid personal data**: keep props to identifiers and UI context.
4. **Keep props stable**: analytics dashboards rely on consistent event names.

