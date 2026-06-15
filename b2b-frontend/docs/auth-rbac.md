---
title: "Frontend Auth & RBAC model"
description: "_Last update: 2026-04-26._"
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
doc_url: /docs/apps/frontend/docs/auth-rbac
doc_path: apps/frontend/docs/auth-rbac.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Frontend Auth & RBAC model

_Last update: 2026-04-26._

## Overview

Frontend consumes backend-authenticated session claims and enforces route guards with a canonical access level:

- `public`
- `user`
- `client`
- `admin`

Global reference: `docs/architecture/auth-rbac-model.md`.

## Session source

- Backend endpoint: `GET /api/v1/auth/me`
- Transport: cookie-based session (`credentials: include`)
- Composable: `composables/useAuthSession.ts`
- Identity payload exposed to UI: `subject`, `email`, `name`, `picture`, `level`, `roles`

## Login flow (MVP)

1. User reaches `/auth/login` and accepts terms placeholder checkbox in UI.
2. User authenticates through a single Google one-click CTA.
3. Frontend exchanges returned Google ID token via OIDC endpoint (`POST /api/v1/auth/oidc`, provider=`google`).
4. Backend sets `INFERA_ACCESS_TOKEN` and `INFERA_REFRESH_TOKEN` cookies.
5. Frontend reads session via `/api/v1/auth/me` and redirects to `/admin` by default (or `next` query override).

## Route policy

| Route | Required level | Enforcement |
|---|---|---|
| `/` | `public` | none |
| `/admin/*` | `admin` | Nuxt `admin` middleware |

When route guard denies access, middleware redirects to `/auth/login` with explicit context (`error=insufficient_role`, `required`, `current`) so login page can render a warning banner and frontend logs can report required/current role mismatch.

## Admin assignment model

Frontend does not infer admin from client-side data.
Admin elevation is evaluated by backend at each OIDC login token issuance against configured allowlist (`BACKEND_SECURITY_ADMIN_EMAILS`).
