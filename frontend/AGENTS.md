# Nudger Frontend – Agent Guide (Nuxt 4 / Vue 3 / Vuetify)

## Guardrails
- Only implement what is explicitly requested; ask for clarification before expanding scope.
- If you spot a regression risk, pause and ask a question instead of guessing.
- Keep or translate comments to English rather than deleting them.
- Briefly explain the reasoning behind non-trivial changes in your summary/PR.
- Frontend code must respect the existing layering: composables talk to Nuxt server routes, and those routes rely on generated OpenAPI services. Never call downstream services directly from the browser.

## Stack & Tooling
- **Framework**: Nuxt 4 with Vue 3 `<script setup lang="ts">`.
- **UI**: Vuetify 3 (via `vuetify-nuxt-module`) plus Nuxt modules for i18n, image, fonts, icons, Pinia, VueUse, MCP, etc.
- **Node**: >= 20.x, **Package manager**: `pnpm@10.12.1`.
- Generated OpenAPI client lives under `shared/api-client/` and must stay in sync with `front-api`.

## Everyday Commands (no `--offline` flag by default)
- `pnpm install`
- `pnpm dev` – Run the dev server (http://localhost:3000)
- `pnpm build` / `pnpm build:ssr`
- `pnpm preview`
- `pnpm generate`
- `pnpm lint`, `pnpm lint:fix`
- `pnpm format`, `pnpm format:check`
- `pnpm test`
- `pnpm generate:api` – Regenerate OpenAPI client when backend contracts change
- `pnpm preprocess:css` – Refresh XWiki/Bootstrap derived styles

Offline mode is optional troubleshooting; use it only when network access is deliberately disabled.

## Project Structure Highlights
- `app/pages/` – File-based routing (kebab-case filenames). Pages may include lightweight structural wrappers (`<div>`, `<v-container>`, `<v-row>`, etc.) but complex UI should live in components under `app/components/`.
- `app/components/` – Reusable UI building blocks. Prefer PascalCase filenames, but existing `The-hero-*` components remain until refactored; do not rename them casually.
- `app/composables/` – Reusable logic (`useFoo`). Keep them SSR-safe and free of DOM-specific code.
- `app/layouts/`, `app/stores/`, `app/plugins/`, `app/assets/`, `app/utils/` – Follow Nuxt conventions.
- `server/` – Nuxt server routes and middleware that wrap OpenAPI clients.
- `shared/` – Code shared between client and server (generated clients, utils, constants).

## Coding Conventions
- Use Composition API with `<script setup lang="ts">`; avoid class-style components.
- Prefer `ref`, `computed`, `watch`, `useState`, `useFetch`, `useAsyncData`, etc. via Nuxt auto-imports.
- Type everything: rely on interfaces/types; avoid `any`. Use discriminated unions or type guards when needed.
- Keep code SSR-friendly (guard against `window`/`document`).
- Utility helpers in `app/utils` or `shared/utils` should start with `_` (e.g., `_sanitizeHtml`).
- Keep composables thin; delegate heavy logic to server routes or shared utilities.

## Vuetify & Styling Guidance
- Default to Vuetify props, layout grid (`v-container`, `v-row`, `v-col`), and theme tokens.
- Scoped SASS/CSS is allowed when Vuetify tokens alone cannot express the design (hero layouts, animations, etc.). Use BEM-style class names and keep selectors minimal.
- Global styles belong in `app/assets`. Avoid inline styles except for trivial tweaks.
- When integrating CMS/XWiki content, ensure the preprocess step (`pnpm preprocess:css`) stays current.

## Naming & Organization
- Components: prefer PascalCase filenames (`HeroSection.vue`). Existing hyphenated names such as `The-hero-video.vue` are kept as-is; document any rename.
- Composables: `useFeature`, `useBlog`, etc.
- Stores: `useXStore` created via `defineStore`.
- Tests: colocate `*.spec.ts` files near the code under test (or in `tests/`).

## Testing & Quality Gates
Before opening a PR run, at minimum:
- `pnpm lint`
- `pnpm test`
- `pnpm build`
- `pnpm generate` (for static output regressions)
- `pnpm preprocess:css` (if CMS styling changed)
- `pnpm generate:api` whenever backend contracts affecting the generated client changed

Document any intentionally skipped check in your summary/PR.

## Documentation Expectations
- Update or extend README, AGENTS.md, architectural notes, and comments when behaviour changes.
- Keep comments in English; translate legacy ones as you touch the file.

## Front API authentication checklist
- All Nuxt server routes must call `front-api` through the shared configuration helper in `~/app/plugins/api-token.server.ts`. Always use the generated OpenAPI services so that helper can always inject the `X-Shared-Token` header.
- Keep the `MACHINE_TOKEN` runtime value synchronised with the backend's `front.security.shared-token` property; drift breaks machine-to-machine authentication.
- Before modifying authentication-sensitive code:
  1. Confirm the shared helper still wraps every outbound call to `config.apiUrl`.
  2. Re-validate the `MACHINE_TOKEN`/`front.security.shared-token` pairing in local and deployment secrets.
  3. Exercise the affected flows (login, refresh, logout, and generated client calls) to prove headers and cookies remain intact.

## MCP & Developer Tooling
- Ensure the Nuxt MCP server is running on port 3000 when working with Claude Code / Vuetify MCP features (`nuxt dev` already exposes it).
- Claude-specific shortcuts (e.g., `/css-class-validator`) remain available; keep instructions compatible with Claude’s CLAUDE.md expectations.

## When Unsure
- Ask for clarification before introducing new patterns or deviating from these guardrails.
- Prefer incremental changes aligned with existing code style and project architecture.
