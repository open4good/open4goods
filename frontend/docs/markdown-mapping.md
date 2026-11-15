# Markdown document mapping (frontend)

## Usage

The frontend reads generic Markdown documents from the repository root `/docs`
folder and category buying guides from `verticals/src/main/resources/guides`
through the Nuxt Content `docs` source defined in `frontend/content.config.ts`.

Generic documentation routes stay under `/docs/<locale>/<slug>` and are rendered by:

- `app/pages/docs/[...slug].vue` (catch-all page)
- `app/components/docs/DocsPageRenderer.vue` (full page renderer)
- `app/components/docs/DocsInlineRenderer.vue` (embedded renderer)

Buying guides are category-owned content:

- source path: `verticals/src/main/resources/guides/<vertical-id>/<guideSlug>.md`
- public route: `/<verticalHomeUrl>/<guideSlug>`
- renderer: `app/components/docs/BuyingGuideRenderer.vue`
- discovery: `VerticalsConfigService` scans `guides/<vertical-id>/*.md` and
  exposes the guide slugs through the category API.

Example:

- source: `verticals/src/main/resources/guides/tv/meilleur-televiseur-caravane-camping-car.md`
- public URL: `/televiseurs/meilleur-televiseur-caravane-camping-car`

Legacy moved guides under `/docs/fr/guides/<slug>` can redirect to the matching
category URL when a moved Markdown file exists, but new buying guides belong
under `verticals/src/main/resources/guides/<vertical-id>/`.

## Frontmatter contract

Generic docs can expose:

```yaml
---
title: 'Page title'
description: 'SEO summary'
type: 'guide'
tags: ['language:en', 'sample']
updatedAt: '2026-04-12'
draft: false
published: true
navigation: true
requiresAuth: false
layout: 'default'
ogImage: '/images/og/docs.png'
noindex: false
---
```

Buying guides should keep only authored SEO content in frontmatter:

```yaml
---
title: 'Page title'
description: 'SEO summary'
type: 'guide'
tags: ['language:fr', 'guide-achat', '<vertical-id-or-topic>']
updatedAt: '2026-04-12'
---
```

Canonical URLs, robots directives, draft/published/navigation defaults and the
category association are application-owned. The guide category is inferred from
the vertical id directory and resolved to the localized `verticalHomeUrl` for
public routes.

## Language tag behaviour

Language access is enforced through tags and domain language resolution:

- Add one explicit tag: `language:en` or `language:fr`.
- `resolveLocaleFromRequest()` derives `domainLanguage` from hostname.
- `useDocsContent` now filters documents with `isDocVisibleForLocale`:
  - hidden when `published: false`
  - hidden when `draft: true` (outside dev mode)
  - hidden when language tag does not match current `domainLanguage`

This check applies to direct page rendering and list/search/navigation queries, preventing cross-language leaks.

## Sample pages

Sample mapped pages are available at:

- `/docs/en/guides/markdown-mapping-sample`
- `/docs/fr/guides/markdown-mapping-sample`

They demonstrate the required `language:*` tag contract.

## Embedded product widgets

Markdown pages can directly use a small set of Vue components. They are
**auto-imported** (Nuxt `components` config uses `pathPrefix: false`, so the
component file name is the MDC tag name) and every prop is **scalar**
(string/number) - the only kind of value MDC can pass through markdown
attributes. No change to the catch-all page or `DocsPageRenderer` is required.

All of them follow the `ProductEmbed` fetch pattern (`useAsyncData`, SSR-friendly,
pending / empty fallbacks).

### `ProductEmbed`

Inline text link to an internal product page.

Supported identifiers:

- `gtin`
- `brand` + `model`

Default rendering is a text link (`style="text"`) with `size="m"`.
If `brand` + `model` resolves to multiple products, no link is rendered (conservative fallback).

```vue
<ProductEmbed gtin="8806092074061" />
<ProductEmbed brand="Samsung" model="QE55QN90A" size="s" />
```

### `ProductCardEmbed`

Full product card (image, impact score, price rows) - wraps `ProductCard`.

| Prop              | Type                         | Default  | Notes                                                |
| ----------------- | ---------------------------- | -------- | ---------------------------------------------------- |
| `gtin`            | string \| number             | -        | Resolves via `GET /api/products/{gtin}`.             |
| `brand` + `model` | string                       | -        | Fallback resolution via `GET /api/products/resolve`. |
| `size`            | `small` \| `medium` \| `big` | `medium` | Card size.                                           |

```vue
<ProductCardEmbed gtin="8806092074061" size="medium" />
<ProductCardEmbed brand="Roborock" model="S8" size="small" />
```

### `BrandShareChart`

Market-share chart (products per brand) for a vertical - wraps `DatavizChart`
(ECharts). Built from a `terms` aggregation on
`attributes.referentielAttributes.BRAND` via `POST /api/products/search`. Fetched
client-side so the ECharts canvas renders once data resolves.

| Prop       | Type                | Default           | Notes                                          |
| ---------- | ------------------- | ----------------- | ---------------------------------------------- |
| `vertical` | string              | guide context     | `verticalId` to aggregate.                     |
| `type`     | `pie` \| `bar`      | `pie`             | `pie` renders a donut, `bar` a bar chart.      |
| `top`      | number              | `8`               | Max number of brands.                          |
| `metric`   | `count` \| `offers` | `count`           | Bucket counts = number of referenced products. |
| `title`    | string              | localized default | Chart title.                                   |

```vue
<BrandShareChart vertical="aspirateurs-robots" type="pie" top="8" />
<BrandShareChart type="pie" top="8" />
```

### `GuideProductGrid`

Grid of the top N products of a vertical, sorted by impact score by default -
wraps `CategoryProductCardGrid`. Rendered server-side (good for guide SEO).

| Prop             | Type    | Default       | Notes                                                                                                                         |
| ---------------- | ------- | ------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `vertical`       | string  | guide context | `verticalId` to query.                                                                                                        |
| `top`            | number  | `3`           | Number of products.                                                                                                           |
| `sort`           | string  | `ecoscore`    | `ecoscore`/`impact` → `scores.ECOSCORE.value` desc. A raw field mapping with optional `:asc`/`:desc` suffix is also accepted. |
| `subcategory`    | string  | -             | Sub-category id (see below). Lists the same products as the sub-category page.                                                |
| `seeAllCategory` | boolean | `false`       | Without `subcategory`, renders a "Voir tout" link to the vertical category page.                                              |

With `subcategory`, the grid reads the sub-category from the category served by
front-api (`subCategories`, same source as `CategoryPage.vue`) and applies its
`activatedFilters` (same conversion as the category page), so it lists the same
products as the sub-category page. Active offers are always required
(`offersCount >= 1`), the default sort is the impact score (`sort` can still be
overridden), and a "Voir tout" link to the sub-category page is rendered. An
unknown sub-category (or one missing from the vertical) renders nothing and logs
a `console.warn` server-side. Without `subcategory` nothing changes.

Sub-category ids: `id` field of
`verticals/src/main/resources/categories/{vertical}/*.yml` (e.g. `dishwasher_45`,
`tv_55_inches`).

Plausible: a click on a product or on "Voir tout" sends `product-widget-click`
with props `page` (path of the page), `vertical`, `subcategory` (empty without
sub-category) and `target` (`product` or `see-all`). These custom properties must
be declared in the Plausible site settings.

```vue
<GuideProductGrid vertical="aspirateurs-robots" top="3" sort="ecoscore" />
<GuideProductGrid top="3" sort="ecoscore" />
```

In a guide Markdown file:

```md
::guide-product-grid{subcategory="dishwasher_45" top="3"}
::
```

Inside `BuyingGuideRenderer`, `BrandShareChart` and `GuideProductGrid` can omit
`vertical`; they receive the category `verticalId` from guide context. Keep
passing `vertical` for standalone docs pages where no guide context exists.

> Buying guides are typically authored with the `@buying-guide-author` prompt
> (`frontend/.claude/prompts/buying-guide-author.md`), which orchestrates SEO
> research and produces a guide under `docs/fr/<verticalHomeUrl>/`.

## Product widgets in blog articles

Blog posts live in `content/blog/fr/*.md`. Their body is rendered to HTML by
markdown-it (`server/utils/blog-content.ts`), not by MDC: only one component is
supported, `guide-product-grid`, with the same syntax as the guides. Write it
on its own line, where the text deals with the products it shows:

```md
::guide-product-grid{vertical="tv" subcategory="tv_55_inches" top="3"}
::
```

| Attribute     | Required | Notes                                                                                  |
| ------------- | -------- | -------------------------------------------------------------------------------------- |
| `vertical`    | yes      | `verticalId` (no guide context in the blog). `[a-z0-9_-]`, else the widget is dropped. |
| `subcategory` | no       | Sub-category id (see `GuideProductGrid`). `[a-z0-9_-]`, else the widget is dropped.    |
| `top`         | no       | 1 to 12, default `3` (an invalid value falls back to `3`).                             |

Other attributes are ignored. The closing `::` line is optional; a malformed
directive stays plain text, and a directive inside a code block is not
interpreted.

In an article the widget always lists products with an active offer, sorted by
Impact Score, renders nothing when empty, and links "Voir tout" to the
sub-category page, or to the category page without `subcategory`
(`seeAllCategory`). The products are fetched during SSR, so the widget HTML is
in the page source. Clicks send `product-widget-click` (see `GuideProductGrid`).

How it works: a markdown-it block rule turns the directive into an empty
marker (`<div data-product-widget="vertical|subcategory|top">`) built from the
validated values only; `TheArticle.vue` splits the sanitized body on these
markers (and on `[assistant]`) and renders `GuideProductGrid` in their place
(`shared/utils/blog-product-widget.ts`). The rest of the body is unchanged and
still sanitized by DOMPurify.
