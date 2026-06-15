---
title: "Frontend foundations blueprint (Option 1)"
description: "_Last update: 2026-04-22._"
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
doc_url: /docs/apps/frontend/docs/frontend-foundations
doc_path: apps/frontend/docs/frontend-foundations.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Frontend foundations blueprint (Option 1)

_Last update: 2026-04-22._

This document captures the first architectural review for the frontend foundations with two MVP pages:

1. Agent enrollment page.
2. Admin dashboard page listing registered agent nodes.

It is intentionally focused on **API surface mapping**, **OpenAPI client generation**, and **reusable UI/composable primitives**.

## 1) Scope and constraints

- Stack: 
  - **Nuxt 4** (Edge / Compatibility mode)
  - **Vuetify 4** (Design System)
  - **TypeScript** (Strict mode)
  - **OpenAPI Client** (Generated transport layer)
- Quality gates for this phase: lint + typecheck + unit tests.
- UX constraints: SSR/SEO friendly and a11y aligned (WCAG AA pragmatic baseline).

## 2) API surface mapping (UI capability -> backend contract)

The table below maps required frontend capabilities to currently documented backend endpoints.

### Project Structure (App folder)

| Directory | Content | Role |
|---|---|---|
| `assets/` | Styles, tokens, icons | Raw visual assets |
| `components/` | Vue SFCs | UI components (Inf prefix for core) |
| `composables/` | Logic + Repositories | Business logic & data access |
| `domains/` | Models + Mappers | Domain logic & DTO transformation |
| `generated/` | API Client | Auto-generated backend contracts |
| `pages/` | File-based routing | App views |

### Enrollment page (agent-restricted)

| UI capability | Endpoint | Status | Notes |
|---|---|---|---|
| Start enrollment challenge | `POST /api/v1/nodes/challenges` | Missing in current router implementation | Present in architecture contract doc; endpoint not found in router controllers. |
| Confirm challenge (operator action) | `POST /api/v1/nodes/challenges/{challengeId}/confirm` | Missing in current router implementation | Required for approval flow and clear status transition in UI. |
| Submit final enrollment payload | `POST /api/v1/nodes/enroll` | Partially documented, backend ownership unclear from current codebase scan | Documented in API contract docs, but no frontend-ready schema available in current generated types. |
| Poll challenge/enrollment status | `GET /api/v1/nodes/challenges/{challengeId}` (or equivalent) | Missing | Required for deterministic UI state without optimistic guessing. |

### Admin dashboard (registered nodes)

| UI capability | Endpoint | Status | Notes |
|---|---|---|---|
| List registered nodes | `GET /api/v1/admin/nodes` | Implemented (backend contract) | No list endpoint currently exposed for backoffice read use case. |
| Node details | `GET /api/v1/admin/nodes/{nodeId}` | Implemented (backend contract) | Needed for detail panel/drill-down and diagnostics. |
| Node health snapshot | derive from node registry (`lastSeen`, `status`) | Partially available via heartbeat ingest only | `POST /api/v1/nodes/heartbeat` ingests data but no admin query endpoint is exposed. |
| Filter by tier/model/status | query params on list endpoint (proposed) | Missing | Required for practical operations at scale. |

### Existing related endpoints in router

- `POST /api/v1/nodes/heartbeat`
- `GET /api/v1/nodes/attestation/challenge`
- `POST /api/v1/nodes/attestation/challenge`

These are agent-facing mTLS endpoints and not enough for an admin backoffice dashboard by themselves.

## 3) Backend contract actions required before full UI delivery

1. Expose enrollment endpoints in the published OpenAPI contract used by frontend generation.
2. Add read/admin endpoints for node listing and node details.
3. Define stable DTOs for:
   - enrollment challenge lifecycle,
   - enrolled node summary,
   - node status timeline/health projection.
4. Add explicit auth model per endpoint in OpenAPI (`admin`, `agent-enrollment`).

## 4) OpenAPI generation foundation (frontend)

### Decision

- Frontend generation source is `http://localhost:8082/api-docs` by default.
- Generation remains overridable with `BACKEND_OPENAPI_URL`.

### Pipeline expectations

- `pnpm codegen` runs deterministic generation.
- Generated files are treated as read-only artifacts (`*.gen.ts`, `types/backend-openapi.d.ts`).
- Repository wrappers adapt generated contracts to UI-safe view models.

## 5) Reusable component/composable strategy

The strategy is to separate concerns by layer and make all page features composable.

### UI foundation components (Vuetify-first)

- `InfPageHeader` (title, actions, breadcrumbs, i18n keys only)
- `InfAsyncState` (loading/empty/error/success states)
- `InfFilterBar` (search + structured filters + chips)
- `InfDataTable` (server-side pagination/sort, slot-based cell rendering)
- `InfStatus` (semantic status chips with token-backed color mapping)
- `InfForm`/`SchemaRenderer` for contract-driven forms

### Composable foundation

- `useEnrollmentRepository()` + `useEnrollmentFlow()`
- `useNodeRegistryRepository()` + `useNodeList()`
- `useApiRequestState()` (shared request/abort/retry/error mapping)
- `usePaginationQuerySync()` (route query <-> table state)

### Repository wrappers

- `repositories/enrollment.repository.ts`
- `repositories/node-registry.repository.ts`

Wrappers are responsible for:

- keeping OpenAPI transport DTOs isolated,
- mapping into stable frontend domain models,
- normalizing backend errors into i18n-ready keys.

## 6) i18n, accessibility and tokens conventions

- No literal UI copy in SFC templates.
- i18n keys grouped by domain (`enrollment.*`, `admin.nodes.*`, `common.*`).
- Status colors always go through Vuetify semantic colors/theme tokens.
- Interactive elements require clear labels/aria attributes.

## 7) Testing baseline (unit only for now)

Prioritized unit tests:

1. Repository mappers (DTO -> domain model).
2. Composable state transitions (idle/loading/success/error).
3. Reusable component behavior with props and emitted events.

## 8) Definition of done for next implementation step

- OpenAPI contract reachable at `/api-docs` with required enrollment + admin-node endpoints.
- Generated client committed and consumed via repositories.
- Enrollment page scaffolded with contract-driven form states.
- Admin nodes page scaffolded with server table + filters + status badges.
- Unit tests green for repositories/composables touched in the increment.


## 9) Admin nodes implementation decisions (2026-04-22 update)

- Strict namespace for admin node reads: `/api/v1/admin/nodes` and `/api/v1/admin/nodes/{nodeId}`.
- Initial scale target: low cardinality (few nodes), while preserving server-side pagination contract.
- Polling strategy: 5s on active tab for near-real-time status refresh.
- Backoffice auth transport: session cookie support (`credentials: include`) plus backend cookie token resolver.
- Error model: RFC7807 Problem Details mapped in frontend error mapper.

- Operator build instructions template: `apps/frontend/docs/templates/operator-page-template.md`.
