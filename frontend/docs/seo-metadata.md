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
