---
title: "Markdown content mapping (Nuxt frontend)"
description: "Issue reported: `http://localhost:3000/sample` returned an error instead of rendering `content/fr/sample.md`."
tags:
  - documentation
  - vue-content
  - structural
  - frontend
owner: platform
audience: all
language: en
component: frontend
maturity: draft
security_classification: public
doc_url: /docs/apps/frontend/docs/markdown-content-mapping
doc_path: apps/frontend/docs/markdown-content-mapping.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Markdown content mapping (Nuxt frontend)

## Investigation summary (2026-04-26)

Issue reported: `http://localhost:3000/sample` returned an error instead of rendering `content/fr/sample.md`.

Root cause found:

- Route fallback file was `pages/[[...slug]].vue`.
- With Nuxt 4 in this repo, that file did not generate a matching route for `/sample`.
- Router warnings confirmed `No match found for location with path "/sample"`.

Fix applied:

- Use a catch-all route file `pages/[...slug].vue`.
- Keep explicit pages (`/`, `/docs`, `/admin/*`) as first-class routes.

## Opinionated architecture

### Why markdown should remain primary

- Content team can ship new pages without touching Vue page components.
- Frontmatter provides product-level controls (SEO, tags, visibility scope).
- Shared Vue blocks are reusable from markdown when richer UX is needed.

### Route and locale model

- i18n strategy: `prefix_except_default`
- Default locale: `fr`
- Slugs stay identical across locales.
- Missing locale content falls back to default locale content.

## Frontmatter contract

Supported frontmatter attributes (validated in `content.config.ts`):

- `title: string` (required)
- `description: string`
- `tags: string[]`
- `scope: public | admin`
- `seo.title: string`
- `seo.description: string`
- `seo.canonical: string`
- `seo.robots: string`

## Access scopes

- `public`: rendered for all users.
- `admin`: rendering blocked for non-admin sessions (friendly access message + noindex robots policy).

## Shared components (content explorer)

`InfContentExplorer` is now the standard building block for markdown navigation:

1. **Tree-like browser** (`InfContentTree`): hierarchical navigation by slug.
2. **Live index table** (`InfContentSearchTable`): weighted search (title > content > tags).
3. **Tag filtering**: multi-tag filtering with Vuetify combobox chips.

Default integration points:

- `/docs` page (dedicated explorer for public docs).
- `/admin/docs` page and admin dashboard section (explorer filtered to `scope=admin`).
- Markdown pages under `/docs/*` render explorer context above document content.

## Filesystem convention

```text
content/
  en/
    sample.md
    docs/*.md
    blog/*.md
    changelog/*.md
  fr/
    sample.md
    docs/*.md
    blog/*.md
    changelog/*.md
```

## Editorial workflow

1. Create/edit markdown in `content/{locale}/...`.
2. Keep frontmatter complete (especially `title`, `description`, `tags`, `scope`, `seo`).
3. Preserve slug parity across locales.
4. Validate in `/docs` explorer before opening PR.
