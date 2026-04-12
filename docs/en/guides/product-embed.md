---
title: "Product embed in markdown"
description: "Embed internal product links in markdown using GTIN or brand/model identifiers."
type: "guide"
tags: ["language:en", "frontend", "markdown", "product"]
weight: 40
updatedAt: "2026-04-12"
draft: false
published: true
navigation: true
---

# Product embed in markdown

Use `ProductEmbed` directly in docs markdown content.

## Identifier modes

Supported identifiers:

- `gtin`
- `brand` + `model`

When `brand` + `model` are ambiguous, the component follows a conservative policy and does **not** render a product link.

## Default behavior

- `style`: `text`
- `size`: `m`
- visible label: `BRAND - Model` when available
- hover title: best available long-name fallback chain

## Examples

```vue
<ProductEmbed gtin="8806092074061" />
```

```vue
<ProductEmbed brand="Samsung" model="QE55QN90A" size="s" />
```

```vue
<ProductEmbed gtin="8806092074061" size="l" />
```
