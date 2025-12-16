# Nudger Frontend – Agent Guide (Nuxt 4 / Vue 3 / Vuetify)

## Guardrails
- Only implement what is explicitly requested; ask for clarification before expanding scope.
- If you spot a regression risk, pause and ask a question instead of guessing.
- Frontend code must respect the existing layering: composables talk to Nuxt server routes, and those routes rely on generated OpenAPI services. Never call downstream services directly from the browser.

## Stack & Tooling
- **Framework**: Nuxt 4 with Vue 3 `<script setup lang="ts">`.
- **UI**: Vuetify 3 (via `vuetify-nuxt-module`) plus Nuxt modules for i18n, image, fonts, icons, Pinia, VueUse, MCP, etc.
- **Node**: >= 20.x, **Package manager**: `pnpm@10.12.1`.
- Generated OpenAPI client lives under `shared/api-client/` and must stay in sync with `front-api`.
  - Doc: [docs/backend-services.md](Doc-services-usage)

## Everyday Commands (use `--offline` flag by default)
- `pnpm install`
- `pnpm dev` – Run the dev server (http://localhost:3000)
- `pnpm build`
- `pnpm generate`
- `pnpm lint`, `pnpm lint:fix`
- `pnpm test`

## Project Structure Highlights
- `app/pages/` – File-based routing (kebab-case filenames). Pages may include lightweight structural wrappers (`<div>`, `<v-container>`, `<v-row>`, etc.) but complex UI should live in components under `app/components/`.
- `app/components/` – Reusable UI building blocks. Prefer PascalCase filenames, but existing `The-hero-*` components remain until refactored; do not rename them casually.
- `app/composables/` – Reusable logic (`useFoo`). Keep them SSR-safe and free of DOM-specific code.
- `app/layouts/`, `app/stores/`, `app/plugins/`, `app/assets/`, `app/utils/` – Follow Nuxt conventions.
- `server/` – Nuxt server routes and middleware that wrap OpenAPI clients.
- `shared/` – Code shared between client and server (generated clients, utils, constants).
- Theme-scoped visuals (logos, hero/parallax backgrounds, textures) live under `app/assets/themes/<theme>/` with `common/` fallbacks—resolve them via `useThemedAsset`/`useThemeAsset` so light/dark switching stays consistent.

### Theme-scoped assets
- Theme-aware visuals live in `app/assets/themes/<theme>/`, with shared fallbacks in `app/assets/themes/common/` (see `app/assets/themes/README.md`).
- Resolve URLs with `useThemedAsset`/`useThemeAsset` instead of hardcoding `/images/...` so Nuxt bundles the correct variant per theme. This applies to backgrounds (e.g., parallax, hero textures) as well as logos/favicons.

## Coding Conventions
- Use Composition API with `<script setup lang="ts">`; avoid class-style components.
- Prefer `ref`, `computed`, `watch`, `useState`, `useFetch`, `useAsyncData`, etc. via Nuxt auto-imports.
- Type everything: rely on interfaces/types; avoid `any`. Use discriminated unions or type guards when needed.
- Keep code SSR-friendly (guard against `window`/`document`).
- Keep composables thin; delegate heavy logic to server routes or shared utilities.
- All static texts must be internationalised
- Also write or update tests (component-level tests) if pertinent


## Vuetify & Styling Guidance
- Always prefer using vuetify components 
- Design must be thinked mobile first, and be responsiv.
- Default to Vuetify props, layout grid (`v-container`, `v-row`, `v-col`), and theme tokens.
- Scoped SASS/CSS is allowed when Vuetify tokens alone cannot express the design (hero layouts, animations, etc.). Use BEM-style class names and keep selectors minimal.
- Global styles belong in `app/assets`. Avoid inline styles except for trivial tweaks.
- Always mutualize styles when possible
- When integrating CMS/XWiki content, ensure the preprocess step (`pnpm preprocess:css`) stays current.

### components mutualisation
- Always consider mutualising ui components
- For example, impact score visualisation must render through `~/components/shared/ui/ImpactScore.vue` to keep the UX consistent across the application. If you need additional behaviour, extend the component via props instead of duplicating markup.

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


## Naming & Organization
- Components: prefer PascalCase filenames (`HeroSection.vue`). Existing hyphenated names such as `The-hero-video.vue` are kept as-is; document any rename.
- Composables: `useFeature`, `useBlog`, etc.
- Stores: `useXStore` created via `defineStore`.
- Tests: colocate `*.spec.ts` files near the code under test (or in `tests/`).

## Testing & hardening
Every code that is produced MUST me validated using :
- `pnpm lint`
- `pnpm test` -> Can run partial tests if no shared components are updated
- `pnpm generate` 

## Documentation Expectations
- Keep comments in English; translate legacy ones as you touch the file.


## I18n localisation
- Every static strings  in .vue pages must be loacalized through  the i18n resources.


## SEO metadata policy
- Every page must declare SEO metadata (title, description, Open Graph fields) and those strings have to be internationalised through the i18n resources.  Twitter-specific tags are not allowed.

## Backend API authentication and caching guidelines
- Instantiate backend OpenAPI clients through `createBackendApiConfig()` from the shared API client utilities. The helper injects the `X-Shared-Token` header and fails fast when `MACHINE_TOKEN` is absent—never call `new Configuration()` directly.
- Keep the `MACHINE_TOKEN` runtime value synchronised with the backend's `front.security.shared-token` property; drift breaks machine-to-machine authentication.
- Every server route that returns backend data is domain-sensitive. Apply caching headers through `setDomainLanguageCacheHeaders(event, cacheControl)` from `server/utils/cache-headers` so both `Cache-Control` and the host-aware `Vary` header are set together. Do **not** set either header manually.

## DomainLanguage injection
Every downstream call should be wrapped in a thin service that:

1. Injects the runtime configuration with `createBackendApiConfig()` so the
   `X-Shared-Token` header is always present.
2. Accepts the caller's domain language (`'en' | 'fr'`) and forwards it to the
   backend.
3. Lazily instantiates the generated API so the client only exists on the
   server (or in Vitest) and can be reused across calls.
4. Guards against accidental client-side usage to keep secrets such as
   `MACHINE_TOKEN` out of the browser bundle.

## Admin access helpers
- Admin-only UI such as the category filters rely on `hasAdminAccess` from
  `shared/utils/_roles.ts`.
- `hasAdminAccess` compares the authenticated user's roles with
  `config.public.editRoles` (defaults to `ROLE_SITEEDITOR,XWIKIADMINGROUP`).
- Update the `EDITOR_ROLES` environment variable if new roles must be allowed;
  avoid hardcoding role names anywhere else.

## When Unsure
- Prefer incremental changes aligned with existing code style and project architecture.
- Ask for clarification before introducing new patterns or deviating from these guardrails.
