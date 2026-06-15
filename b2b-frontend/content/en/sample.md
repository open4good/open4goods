---
title: "Sample page"
description: "This page is rendered from `content/en/sample.md`."
tags:
  - documentation
  - vue-content
  - frontend
owner: platform
audience: all
language: en
component: frontend
maturity: draft
security_classification: public
doc_url: /docs/apps/frontend/content/en/sample
doc_path: apps/frontend/content/en/sample.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Sample page

This page is rendered from `content/en/sample.md`.

<InfMarkdownCallout title="Embedded Vue component" tone="success">
You can embed Vue components directly in markdown for custom rich sections.
</InfMarkdownCallout>

## Route mapping

- `/sample` resolves to the default locale (`fr`) and falls back to French content first.
- `/en/sample` resolves to this English content file.
- `/en/docs` now includes a shared documentation browser (tree + table + tags).
