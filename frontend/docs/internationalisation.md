# Frontend internationalisation

## Overview
The Nuxt 3 frontend determines the active language on every request by inspecting the incoming hostname. This logic is centralised in [`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts) so that both client and server share the same mapping between hostnames, domain language codes (`'en' | 'fr'`), and Nuxt locales (`'en-US' | 'fr-FR'`).

The helper is consumed by:

- [`app/plugins/i18n-hostname.ts`](../app/plugins/i18n-hostname.ts) to keep SSR and CSR aligned when switching locales.
- Server API handlers (e.g. [`server/api/blog/articles.ts`](../server/api/blog/articles.ts)) to forward the resolved `domainLanguage` to backend services such as `BlogApi` and `ContentApi`.

The i18n module keeps the `no_prefix` routing strategy. Hostnames remain the single source of truth: users navigate unprefixed paths (for example `/produits`) and the helper ensures that the locale matches the expected language for the current domain on every request.

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
The mapping lives in [`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts) across two constants: `HOST_DOMAIN_LANGUAGE_MAP` and `DOMAIN_LANGUAGE_TO_LOCALE_MAP`. To add or change domains:

1. Update `HOST_DOMAIN_LANGUAGE_MAP` so that each hostname points to the appropriate domain language (`'en' | 'fr'`).
2. When introducing a new language, also extend `DOMAIN_LANGUAGE_TO_LOCALE_MAP` with the Nuxt locale string.
3. Keep hostnames lowercase and without protocol or trailing slash; ports are removed automatically during normalisation.
4. Restart the Nuxt server if it is already running so the updated map is picked up.

When introducing a new locale, also register it in `nuxt.config.ts` under the `i18n.locales` array so translations can load correctly.

## Behaviour on unknown domains
If the application receives a hostname that is not present in `HOST_DOMAIN_LANGUAGE_MAP`, the request falls back to English (`domainLanguage: 'en'`, `locale: 'en-US'`). Server-side callers log a warning describing the unknown hostname so operators can adjust the mapping. Client-side navigation continues without additional logging to avoid noise in the browser console.

## Relationship with content bundles
Locale codes correspond to the translation bundles stored under `frontend/i18n/`. Each domain listed above automatically loads the matching bundle; there is no need for query parameters or path prefixes. Manual language switching widgets should respect the hostname contract—if a different behaviour is needed, adjust the shared helper first so SSR, CSR, and server-to-server calls remain aligned.
