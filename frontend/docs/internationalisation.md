# Frontend internationalisation

## Overview

The Nuxt 3 frontend determines the active language on every request by inspecting the incoming hostname. This logic is centralised in [`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts) so that both client and server share the same mapping between hostnames, domain language codes (`'en' | 'fr'`), and Nuxt locales (`'en-US' | 'fr-FR'`). The helper now also exposes `buildI18nLocaleDomains()`, allowing [`nuxt.config.ts`](../nuxt.config.ts) to hydrate each locale definition with its canonical domain plus any alternates (e.g. `localhost`) without duplicating configuration. With `differentDomains: true` enabled, the i18n module can reuse the same mapping for host-based locale detection while our plugin keeps SSR and CSR aligned. The default locale is French (`DEFAULT_DOMAIN_LANGUAGE = 'fr'` / `DEFAULT_NUXT_LOCALE = 'fr-FR'`) to match the live site at `nudger.fr`.

The helper is consumed by:

- [`app/plugins/i18n-hostname.ts`](../app/plugins/i18n-hostname.ts) to keep SSR and CSR aligned when switching locales.
- Server API handlers (e.g. [`server/api/blog/articles.ts`](../server/api/blog/articles.ts)) to forward the resolved `domainLanguage` to backend services such as `BlogApi` and `ContentApi`.

The i18n module keeps the `no_prefix` routing strategy. Hostnames remain the single source of truth: users navigate unprefixed paths (for example `/produits`) and the helper ensures that the locale matches the expected language for the current domain on every request.

## Localised route slugs

Top-level routes now expose translated slugs per locale through [`shared/utils/localized-routes.ts`](../shared/utils/localized-routes.ts). The helper centralises the mapping between route names and locale-specific paths so that:

- Navigation components (`The-hero-menu.vue`, `The-mobile-menu.vue`, etc.) can render the correct links for the active host.
- Programmatic navigations (for instance, blog article cards) reuse the same mapping to avoid hard-coded `/blog/...` URLs.
- `nuxt.config.ts` derives the `@nuxtjs/i18n` `pages` configuration from the shared table, ensuring that `/notre-blog` resolves to the blog index on the French hostname while `/our-blog` serves the English version. Because the module runs with `customRoutes: 'config'`, those slugs are now registered as real route aliases so visiting the translated path no longer results in a 404.

### Defining new localized routes

1. Pick a stable route name for the page (e.g. the `team` page). Nuxt will use the file-based name (`pages/team.vue` → `team`).
2. Update `LOCALIZED_ROUTE_PATHS` so each locale maps to the desired slug. Slugs must start with `/` and may include dynamic parameters using the Nuxt syntax (`/blog/[slug]`).
3. Consume `resolveLocalizedRoutePath(routeName, locale, params?)` wherever a link is generated. The helper accepts optional params for dynamic segments—`resolveLocalizedRoutePath('blog-slug', 'fr-FR', { slug })` renders `/blog/${slug}` with URL encoding applied automatically.
4. Import `normalizeLocale(locale)` when dealing with untrusted input. It coerces unknown locales back to the default (`DEFAULT_NUXT_LOCALE`) so navigation never breaks.

### Sharing the configuration with Nuxt i18n

`buildI18nPagesConfig()` exports the same mapping as a structure that Nuxt i18n understands. `nuxt.config.ts` feeds this output to the `pages` option, ensuring the module registers translated aliases and generates `<link rel="alternate">` tags for SEO. Keeping the data in one place guarantees SSR, CSR, and server routes all agree on which slug belongs to which locale.

## Current hostname mapping

| Hostname     | Domain language | Nuxt locale | Notes                                         |
| ------------ | --------------- | ----------- | --------------------------------------------- |
| `nudger.fr`  | `fr`            | `fr-FR`     | Default production site and canonical locale. |
| `nudger.com` | `en`            | `en-US`     | English production domain.                    |
| `localhost`  | `fr`            | `fr-FR`     | Development override for local browsers.      |
| `127.0.0.1`  | `en`            | `en-US`     | Development override for English testing.     |

_IPv6 loopback (`::1`) is intentionally ignored so that dual-stack machines fall back to the default French locale._

## How the helper works

1. **Hostname normalisation** – `normalizeHost` extracts the first value from incoming headers (supporting comma-separated `x-forwarded-host` values), strips the port, and lowercases it so that `LOCALHOST:3000` resolves to `localhost`.
2. **Domain language resolution** – the hostname is matched against `HOST_DOMAIN_LANGUAGE_MAP`. When no match is found the helper falls back to French (`domainLanguage: 'fr'`, `locale: 'fr-FR'`).
3. **Locale derivation** – `DOMAIN_LANGUAGE_TO_LOCALE_MAP` provides the Nuxt locale string associated with each domain language.
4. **Observability** – server callers can enable logging (default behaviour) to warn about unknown hostnames. Client-side consumers typically disable it to avoid console noise.
5. **Application** – the i18n plugin uses `setLocale` only when the resolved locale differs from the current one. Server routes pass the `domainLanguage` to service factories so outbound API calls carry the correct locale context.

## Updating or extending the mapping

The mapping lives in [`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts) across two constants: `HOST_DOMAIN_LANGUAGE_MAP` and `DOMAIN_LANGUAGE_TO_LOCALE_MAP`. To add or change domains:

1. Update `HOST_DOMAIN_LANGUAGE_MAP` so that each hostname points to the appropriate domain language (`'en' | 'fr'`).
2. When introducing a new language, also extend `DOMAIN_LANGUAGE_TO_LOCALE_MAP` with the Nuxt locale string.
3. Keep hostnames lowercase and without protocol or trailing slash; ports are removed automatically during normalisation.
4. Restart the Nuxt server if it is already running so the updated map is picked up.

When introducing a new locale, also register it in `nuxt.config.ts` under the `i18n.locales` array so translations can load correctly.

## Behaviour on unknown domains

If the application receives a hostname that is not present in `HOST_DOMAIN_LANGUAGE_MAP`, the request falls back to French (`domainLanguage: 'fr'`, `locale: 'fr-FR'`). Server-side callers log a warning describing the unknown hostname so operators can adjust the mapping. Client-side navigation continues without additional logging to avoid noise in the browser console.

## Relationship with content bundles

Locale codes correspond to JSON translation bundles stored under
`frontend/i18n/locales/*.json`. Nuxt i18n lazy-loads thin TypeScript wrappers
(`frontend/i18n/locales/*.ts`) that re-export those JSON messages alongside the
Vuetify locale pack for the same language. This allows the project to keep JSON
as the authoring format while Vuetify components receive translated UI strings
through `$vuetify`.

When adding or updating a locale:

1. Modify the JSON file to adjust application messages.
2. Ensure a sibling `.ts` wrapper imports both the JSON bundle and the relevant
   Vuetify pack from `vuetify/locale`, then spreads them into the default export.
3. Register the locale in `nuxt.config.ts` (file points to the `.ts` wrapper) and
   extend `i18n.config.ts` if the locale should be available at runtime.

Each domain listed above automatically loads the matching bundle; there is no
need for query parameters or path prefixes. Manual language switching widgets
should respect the hostname contract—if a different behaviour is needed, adjust
the shared helper first so SSR, CSR, and server-to-server calls remain aligned.
