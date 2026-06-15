---
title: "Admin dashboard shell"
description: "This increment introduces a dedicated `/admin` overview route and a left navigation drawer for all `/admin/*` pages."
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
doc_url: /docs/apps/frontend/docs/admin-dashboard
doc_path: apps/frontend/docs/admin-dashboard.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Admin dashboard shell

This increment introduces a dedicated `/admin` overview route and a left navigation drawer for all `/admin/*` pages.

## What was added

- Persistent admin sidebar (in `layouts/default.vue`) for:
  - `/admin`
  - `/admin/nodes`
  - `/admin/keys`
  - `/admin/organisations`
  - `/admin/users`
- Synthetic admin overview page (`pages/admin/index.vue`) with:
  - Fleet node stats
  - API key summary
  - Organization count
  - User count
  - Quick actions linking to existing admin sections

## Data sources (real API)

Dashboard cards use existing repositories and backend endpoints:

- `GET /api/v1/admin/nodes/stats`
- `GET /api/v1/admin/keys/summary?days=7`
- `GET /api/v1/admin/organizations?page=0&size=1`
- `GET /api/v1/admin/users?page=0&size=1`

## Notes

- No admin role-based menu variance is applied for now.
- Existing product branding and Vuetify components are reused.
