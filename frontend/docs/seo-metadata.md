# SEO metadata generation

This document describes how the Nuxt frontend consumes SEO metadata and where
fallbacks are still generated locally.

## Product pages

Product pages use backend data as the source of truth for user-facing SEO
metadata. The `ProductNamesDto` contract exposes:

- `metaTitle`: backend-computed SERP title.
- `metaDescription`: backend-computed meta description.
- `ogTitle`: backend-computed Open Graph title.
- `ogDescription`: backend-computed Open Graph description.

The frontend keeps local product templates only as resilience fallbacks when a
legacy API response or partially populated product does not contain these
fields. New product title improvements should be implemented in `front-api`, not
in `ProductPage.vue`.

### Title, description and H1 (GOU-1181)

`front-api` `ProductSeoMetaService` is the single source of truth for the
product `<title>` and meta description; the page renders them as-is.

- **Title**, at most 60 characters, never cut by an ellipsis:
  `<Brand> <Model> : <type> <key features> - avis, prix et impact` (the dash
  before the suffix is an en dash, U+2013). The type is
  the first alternative of the vertical URL prefix (`lave-vaisselle`), the
  features come from the vertical `popularAttributes` that carry a unit or a
  suffix (`8 kg`, `44 dB`). The manufacturer reference number some sources
  append to the model (`E62LB200S 911572124`) is removed, and brands written in
  capitals are title-cased (`ELECTROLUX` → `Electrolux`, acronyms such as `LG`
  are kept). Degradation order when too long: second feature, long suffix
  (`- avis et prix`), first feature, type, suffix. Examples:
  `Electrolux E62LB200S : lave-vaisselle - avis, prix et impact`,
  `Hisense WD3I8043BBF2 : lave-linge 8 kg - avis et prix`.
- **Description**, at most 160 characters: impact score out of 20, two key
  features and the price range of the current offers
  (`prix de 454,99 à 699,99 €`). A missing score, feature or offer is omitted,
  never invented; facts are dropped from the end when too long.
- **H1**: `names.pageTitle` (brand and model, without manufacturer reference),
  rendered once by `ProductHero` (`title-tag="h1"`), without the vertical plural
  prefix, on mobile and desktop.

### Alternatives

`ProductAlternatives` fetches its default selection through `useAsyncData`
(key: vertical, GTIN and active filters), so the alternative product cards and
their `<a href>` links are in the SSR HTML and crawlable. Toggling a filter chip
changes the key and refetches on the client.

When the initial search returns fewer than 3 alternatives (current product
excluded), the default filters are relaxed in a fixed order, stopping as soon as
3 are found: (1) all popular-attribute filters, (2) the price, (3) the
ecoscore, i.e. no filter at all. The vertical is never changed and a category
with fewer than 3 other products shows what exists; the "best product" message
only appears when there is none. The chips show the filter set actually
applied. After the first chip click there is no automatic relaxation. All steps
run inside the same `useAsyncData` handler (one SSR payload, no refetch on
hydration), so the links stay in the server HTML (GOU-1215).

## Category pages

Category and sub-category pages resolve SEO metadata from backend category DTOs
with visible content as the fallback chain. The shared resolver is
`app/utils/seo/category-meta.ts`.

Resolution order:

1. sub-category SEO/Open Graph fields;
2. sub-category visible title or description;
3. vertical SEO/Open Graph fields;
4. vertical visible title or description;
5. site name for empty titles.

Markdown is stripped from descriptions before writing meta tags.

## Static and editorial pages

Static pages, documentation pages, blog pages and CMS/XWiki pages still provide
metadata from localized frontend resources or their dedicated content APIs. They
should use shared helpers such as `PageHeader`/`useHeaderSeo` where possible and
avoid duplicating canonical, Open Graph and JSON-LD boilerplate.

`useHeaderSeo` registers its title, description, Open Graph and canonical tags
with `tagPriority: 'low'`: they are fallbacks. A page that declares its own
`useSeoMeta`/`useHead` values keeps them, even though `PageHeader` is a child
component whose head entry would otherwise win the deduplication. An empty
header description is omitted instead of rendering an empty meta tag.

## Canonical URLs

Shared header SEO canonical URLs use the request origin and pathname only,
without trailing slash (`/opendata/gtin/` declares `/opendata/gtin`). Query
parameters and fragments are intentionally dropped unless a page implements its
own explicit canonical URL for indexable query states.

A page canonical built with `resolveLocalizedRoutePath` must name a route of
`LOCALIZED_ROUTE_PATHS`; for other file-based routes (for example
`opendata-gtin`) use `useLocalePath()`, otherwise the helper falls back to
`/<route-name>`, a 404 path.

Product pages reached through a legacy or stale URL (`/{gtin}-{slug}` without
category, or an outdated slug) answer a 301 to the product `fullSlug`.

## Rules for future changes

- Backend metadata wins over frontend-generated strings.
- Frontend fallbacks should be small, explicit and documented near their use.
- Twitter-specific tags are not allowed.
- Open Graph fields should be set together with title, description, URL and
  image when available.
