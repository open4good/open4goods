# Frontend internationalisation

## Overview
The Nuxt 3 frontend determines the active language on every request by inspecting the incoming hostname. Locale metadata is centralised in [`shared/config/locales.ts`](../shared/config/locales.ts) so both client and server share the same mapping between hostnames, domain language codes (e.g. `'en'`, `'fr'`), Nuxt locales (e.g. `'en-US'`, `'fr-FR'`), and Vuetify's translated UI strings. [`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts) derives its lookups from these definitions, while [`nuxt.config.ts`](../nuxt.config.ts) consumes them to populate the `@nuxtjs/i18n` module with locale metadata and domain aliases. With `differentDomains: true` enabled, the i18n module can reuse the same mapping for host-based locale detection while our plugin keeps SSR and CSR aligned.

The helper is consumed by:

- [`app/plugins/i18n-hostname.ts`](../app/plugins/i18n-hostname.ts) to keep SSR and CSR aligned when switching locales.
- [`app/plugins/vuetify-locale.ts`](../app/plugins/vuetify-locale.ts) which merges the Vuetify-provided locale bundles declared in `LOCALE_DEFINITIONS` into Vue I18n so components never warn about missing `$vuetify` keys.
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
| Hostname        | Domain language | Nuxt locale | Notes                                     |
|-----------------|-----------------|-------------|-------------------------------------------|
| `nudger.com`    | `en`            | `en-US`     | Default English site.                     |
| `nudger.fr`     | `fr`            | `fr-FR`     | French production domain.                 |
| `localhost`     | `fr`            | `fr-FR`     | Development override for local browsers.  |
| `127.0.0.1`     | `en`            | `en-US`     | Development override for English testing. |

*IPv6 loopback (`::1`) is intentionally ignored so that dual-stack machines fall back to the default English locale.*

## How the helper works
1. **Hostname normalisation** – `normalizeHost` extracts the first value from incoming headers (supporting comma-separated `x-forwarded-host` values), strips the port, and lowercases it so that `LOCALHOST:3000` resolves to `localhost`.
2. **Domain language resolution** – the hostname is matched against `HOST_DOMAIN_LANGUAGE_MAP`. When no match is found the helper falls back to English (`domainLanguage: 'en'`, `locale: 'en-US'`).
3. **Locale derivation** – `DOMAIN_LANGUAGE_TO_LOCALE_MAP` provides the Nuxt locale string associated with each domain language.
4. **Observability** – server callers can enable logging (default behaviour) to warn about unknown hostnames. Client-side consumers typically disable it to avoid console noise.
5. **Application** – the i18n plugin uses `setLocale` only when the resolved locale differs from the current one. Server routes pass the `domainLanguage` to service factories so outbound API calls carry the correct locale context.

## Updating or extending the mapping
The single source of truth is [`shared/config/locales.ts`](../shared/config/locales.ts). To add or change domains or locales:

1. Import the appropriate Vuetify bundle from `vuetify/locale` if you are introducing a new language.
2. Append or update an entry in `LOCALE_DEFINITIONS`, providing the domain language, Nuxt locale, i18n metadata, list of domains, and Vuetify messages.
3. Keep hostnames lowercase and without protocol or trailing slash; ports are removed automatically during normalisation.
4. Add the corresponding JSON translation file under `i18n/locales/` when introducing a new locale.
5. Restart the Nuxt server if it is already running so the updated map is picked up.

The rest of the stack—domain detection, Nuxt configuration, and the Vuetify locale plugin—picks up the change automatically because they all read from `LOCALE_DEFINITIONS`.

## Behaviour on unknown domains
If the application receives a hostname that is not present in `HOST_DOMAIN_LANGUAGE_MAP`, the request falls back to English (`domainLanguage: 'en'`, `locale: 'en-US'`). Server-side callers log a warning describing the unknown hostname so operators can adjust the mapping. Client-side navigation continues without additional logging to avoid noise in the browser console.

## Relationship with content bundles
Locale codes correspond to the translation bundles stored under `frontend/i18n/`. Each domain listed above automatically loads the matching bundle; there is no need for query parameters or path prefixes. Manual language switching widgets should respect the hostname contract—if a different behaviour is needed, adjust the shared helper first so SSR, CSR, and server-to-server calls remain aligned.