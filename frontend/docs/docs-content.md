# Documentation Markdown (Nuxt Content)

This repository serves documentation from **/docs** at the repo root via
`@nuxt/content` (Option A). The module indexes locale folders directly.

## Structure

```
/docs
  /en
    /impact-score
      overview.md
      methodology.md
  /fr
    /impact-score
      overview.md
      methodology.md
```

## Frontmatter contract

Each markdown file can define:

```yaml
---
title: 'Human title'
description: 'Short SEO summary'
tags: ['tag1', 'tag2']
icon: 'mdi-book-open-page-variant'
weight: 10
updatedAt: '2026-02-10'
draft: false
---
```

## Add a new doc

1. Create the markdown file under `/docs/<locale>/...`.
2. Add frontmatter (title + description recommended).
3. The page becomes available at `/docs/<slug>`.

## Components

### Full page renderer

```vue
<DocsPageRenderer slug-or-path="impact-score/overview" />
```

### Inline renderer

```vue
<DocsInlineRenderer slug-or-path="impact-score/methodology" />
```

### Doc browser (tree + reader)

```vue
<DocBrowser base-path="/docs" />
```

### Cards browser

```vue
<DocCardsBrowser base-path="/docs" />
```

### Search endpoint

The docs browser components call:

```
GET /api/docs/search?locale=en&basePath=/docs/en&query=impact
```

Results are built from the in-memory MiniSearch index on the server.
