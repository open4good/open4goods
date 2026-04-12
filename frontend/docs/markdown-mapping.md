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
title: "Page title"
description: "SEO summary"
type: "guide"
tags: ["language:en", "sample"]
updatedAt: "2026-04-12"
draft: false
published: true
navigation: true
requiresAuth: false
layout: "default"
ogImage: "/images/og/docs.png"
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

Markdown pages can directly use the `ProductEmbed` Vue component.

Supported identifiers:
- `gtin`
- `brand` + `model`

Default rendering is a text link (`style="text"`) with `size="m"`.
If `brand` + `model` resolves to multiple products, no link is rendered (conservative fallback).

Example:

```vue
<ProductEmbed gtin="8806092074061" />
<ProductEmbed brand="Samsung" model="QE55QN90A" size="s" />
```
