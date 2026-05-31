# Markdown document mapping (frontend)

## Usage

The frontend reads Markdown documents from the repository root `/docs` folder through the Nuxt Content `docs` source defined in `frontend/content.config.ts`.

Public routes stay under `/docs/<locale>/<slug>` and are rendered by:

- `app/pages/docs/[...slug].vue` (catch-all page)
- `app/components/docs/DocsPageRenderer.vue` (full page renderer)
- `app/components/docs/DocsInlineRenderer.vue` (embedded renderer)

## Frontmatter contract

Each doc can expose:

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
| `vertical` | string              | -                 | `verticalId` to aggregate.                     |
| `type`     | `pie` \| `bar`      | `pie`             | `pie` renders a donut, `bar` a bar chart.      |
| `top`      | number              | `8`               | Max number of brands.                          |
| `metric`   | `count` \| `offers` | `count`           | Bucket counts = number of referenced products. |
| `title`    | string              | localized default | Chart title.                                   |

```vue
<BrandShareChart vertical="aspirateurs-robots" type="pie" top="8" />
```

### `GuideProductGrid`

Grid of the top N products of a vertical, sorted by impact score by default -
wraps `CategoryProductCardGrid`. Rendered server-side (good for guide SEO).

| Prop       | Type   | Default    | Notes                                                                                                                         |
| ---------- | ------ | ---------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `vertical` | string | -          | `verticalId` to query.                                                                                                        |
| `top`      | number | `3`        | Number of products.                                                                                                           |
| `sort`     | string | `ecoscore` | `ecoscore`/`impact` → `scores.ECOSCORE.value` desc. A raw field mapping with optional `:asc`/`:desc` suffix is also accepted. |

```vue
<GuideProductGrid vertical="aspirateurs-robots" top="3" sort="ecoscore" />
```

> Buying guides are typically authored with the `@buying-guide-author` prompt
> (`frontend/.claude/prompts/buying-guide-author.md`), which orchestrates SEO
> research and produces a guide under `docs/fr/guides/`.
