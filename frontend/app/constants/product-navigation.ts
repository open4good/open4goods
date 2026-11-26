/**
 * Breakpoint below which the product page shows its summary navigation as a
 * horizontal bar above the content instead of a sticky sidebar.
 *
 * The layout itself is driven by `max-width: 1279.98px` media queries in
 * `ProductPage.vue` and `ProductSummaryNavigation.vue` (keep them in sync), so
 * the server HTML never depends on the viewport. Script code only reads this
 * query for behaviour, after mount. It is not Vuetify's `lg` (1145px in
 * Vuetify 4), whose `useDisplay` reports a 0px width during SSR.
 */
export const PRODUCT_COMPACT_NAVIGATION_MEDIA_QUERY = '(max-width: 1279.98px)'
