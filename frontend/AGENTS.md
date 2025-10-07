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
  - Doc: [docs/backend-services.md](Doc-services-usage)

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

### Design token glossary
The theme palette exposes the following generic tokens. Use `rgb(var(--v-theme-<token>))` for solid fills and `rgba(var(--v-theme-<token>), <alpha>)` when translucency is needed.

| Token | Intent | Light | Dark |
| --- | --- | --- | --- |
| `hero-gradient-start` | Primary hero gradient leading stop | `#1976D2` | `#1E3A8A` |
| `hero-gradient-mid` | Mid hero stop for translucent overlays | `#1976D2` | `#1D4ED8` |
| `hero-gradient-end` | Accent hero gradient stop | `#43A047` | `#166534` |
| `hero-overlay-strong` | Intense hero highlight overlays | `#FFFFFF` | `#FFFFFF` |
| `hero-overlay-soft` | Soft hero glassmorphism overlay | `#FFFFFF` | `#FFFFFF` |
| `hero-pill-on-dark` | Eyebrow/label tint on dark surfaces | `#FFFFFF` | `#FFFFFF` |
| `surface-default` | Default surface/background | `#FFFFFF` | `#0F172A` |
| `surface-muted` | Muted section backgrounds | `#F8FAFC` | `#111827` |
| `surface-alt` | Alternate neutral surface | `#EEF4FA` | `#1E293B` |
| `surface-glass` | Translucent cards / tiles | `#F4F7FA` | `#1E293B` |
| `surface-glass-strong` | Elevated glass cards (forms) | `#FBFCFD` | `#111827` |
| `surface-primary-050` | Primary wash for empty states | `#F4F8FD` | `#0B1220` |
| `surface-primary-080` | Subtle primary border/background | `#EDF4FB` | `#13213B` |
| `surface-primary-100` | Stronger primary-tinted fill | `#E8F1FB` | `#1B2A44` |
| `surface-primary-120` | Icon/avatar tint | `#E3EFFA` | `#22304C` |
| `surface-ice-050` | Light ice wash for long sections | `#EEF4FA` | `#152238` |
| `surface-ice-100` | Stronger ice gradient stop | `#F5FAFF` | `#0F172A` |
| `surface-muted-contrast` | Neutral footer/metadata surface | `#F5F5F5` | `#1F2937` |
| `border-primary-strong` | Brand-accented outlines | `#C6DDF4` | `#1E40AF` |
| `shadow-primary-600` | Brand-tinted drop shadow | `#1976D2` | `#3B82F6` |
| `text-neutral-strong` | Primary body text | `#101828` | `#F8FAFC` |
| `text-neutral-secondary` | Supporting body text | `#475467` | `#CBD5F5` |
| `text-neutral-soft` | Quiet helper text | `#667085` | `#94A3B8` |
| `text-on-accent` | Text atop accent surfaces | `#152E49` | `#E2E8F0` |
| `accent-primary-highlight` | Brand highlight strokes/glows | `#2196F3` | `#38BDF8` |
| `accent-supporting` | Secondary accent/CTA highlight | `#4CAF50` | `#22C55E` |

Legacy `team-*` tokens remain for backwards compatibility; prefer the generic names above for new work so contact, blog, and future pages can share a single palette.

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

> **Test updates**: Whenever you author or modify spec/test files, run `pnpm generate`, `pnpm lint` and `pnpm build` in addition to `pnpm test` to catch type and generation regressions early.

## Documentation Expectations
- Update or extend README, AGENTS.md, architectural notes, and comments when behaviour changes.
- Keep comments in English; translate legacy ones as you touch the file.

## SEO metadata policy
- Only include Open Graph meta tags when defining SEO metadata. Twitter-specific tags are not allowed.
- Every page must declare SEO metadata (title, description, Open Graph fields) and those strings have to be internationalised through the i18n resources.

## Backend API authentication guidelines
- Instantiate backend OpenAPI clients through `createBackendApiConfig()` from the shared API client utilities. The helper injects the `X-Shared-Token` header and fails fast when `MACHINE_TOKEN` is absent—never call `new Configuration()` directly.
- Only create backend API instances during SSR/Vitest execution (e.g. inside Nuxt event handlers). Services such as `useBlogService`, `useContentService`, and `useTeamService` must lazily obtain their API via the helper to keep the shared token out of browser bundles.
- Keep the `MACHINE_TOKEN` runtime value synchronised with the backend's `front.security.shared-token` property; drift breaks machine-to-machine authentication.
- Before modifying authentication-sensitive code:
  1. Confirm every outbound call that targets `config.apiUrl` uses the helper-backed configuration or the `api-token.server` plugin so headers are injected server-side only.
  2. Re-validate the `MACHINE_TOKEN`/`front.security.shared-token` pairing in local and deployment secrets.
  3. Exercise the affected flows (login, refresh, logout, and generated client calls) to prove headers and cookies remain intact.

## MCP & Developer Tooling
- Ensure the Nuxt MCP server is running on port 3000 when working with Claude Code / Vuetify MCP features (`nuxt dev` already exposes it).
- Claude-specific shortcuts (e.g., `/css-class-validator`) remain available; keep instructions compatible with Claude’s CLAUDE.md expectations.

## When Unsure
- Ask for clarification before introducing new patterns or deviating from these guardrails.
- Prefer incremental changes aligned with existing code style and project architecture.
