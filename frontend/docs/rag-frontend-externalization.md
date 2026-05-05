# Frontend RAG externalization plan

## Goal

Create a reusable RAG component package while preserving current docs UX (`/docs` and `/docs/[...slug]`).

## Recommended packaging

1. `frontend/app/components/docs-rag/` for UI building blocks
2. `frontend/app/composables/useRagClient.ts` for client-side orchestration
3. `frontend/server/api/rag/*` routes as the only backend entrypoint
4. Optional extraction later to a workspace package when APIs stabilise

## Contracts

- `RagChatPanel` should consume a typed query/response contract only.
- Metadata-driven hero/summary sections should consume a dedicated metadata DTO.
- Missing metadata must fail loudly in development and fallback gracefully in production.

## Suggested first refactor steps

1. Split `RagChatPanel.vue` into pure-presentational + orchestration wrapper.
2. Introduce shared `RagDocumentSummaryCard` and `RagSourceList` components.
3. Centralize request/stream/error handling in `useRagClient`.
4. Add schema validation in server routes before returning data to components.

## Test strategy

- Component tests for error/loading/sources rendering.
- Route-level tests for metadata validation and fallback behavior.
- Snapshot checks for docs page hero/summary rendering.
