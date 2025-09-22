# Frontend internationalisation

## Overview
The Nuxt 3 frontend determines the active locale on every request by inspecting the incoming hostname. The logic lives in [`app/plugins/i18n-hostname.ts`](../app/plugins/i18n-hostname.ts) and runs during both server-side rendering and client-side navigation, guaranteeing that the page is rendered in the correct language even on the first SSR pass.

The i18n module is configured with the `no_prefix` routing strategy. As a consequence, the hostname is the single source of truth: users always see unprefixed paths (for example `/produits`) and the plugin ensures that the locale matches the expected language for the current domain.

## Current hostname-to-locale mapping
| Hostname        | Locale | Notes                                     |
|-----------------|--------|-------------------------------------------|
| `nudger.com`    | `en-US`| Default English site.                     |
| `nudger.fr`     | `fr-FR`| French production domain.                 |
| `localhost`     | `fr-FR`| Development override for local browsers.  |
| `127.0.0.1`     | `en-US`| Development override for English testing. |

*IPv6 loopback (`::1`) is intentionally ignored so that dual-stack machines fall back to the default English locale.*

## How the plugin works
1. **Hostname normalisation** – the plugin extracts the first value from the `x-forwarded-host` header (or `host` as a fallback) on the server, and uses `window.location.host` in the browser. Ports are stripped so that `localhost:3000` still resolves to `localhost`.
2. **Locale resolution** – the hostname is matched against the static map shown above. When no match is found, the locale falls back to `en-US`.
3. **SSR observability** – on the server, unmatched hostnames trigger a warning log entry (`console.warn`) before applying the fallback. This makes misconfigurations visible in hosting logs without interrupting the response.
4. **Locale application** – the plugin compares the resolved locale with the current one provided by `@nuxtjs/i18n` and calls `setLocale` only when a change is required.

## Updating or extending the mapping
The mapping is defined in the `HOST_LOCALE_MAP` constant inside [`app/plugins/i18n-hostname.ts`](../app/plugins/i18n-hostname.ts). To add or change domains:

1. Edit the object so that each hostname points to the desired locale code (e.g. `'shop.example.com': 'fr-FR'`).
2. Keep hostnames lowercase and without protocol or trailing slash.
3. For development overrides, add entries for raw hostnames only—ports are automatically removed.
4. Restart the Nuxt server if it is already running so the updated map is picked up.

When introducing a new locale, also register it in `nuxt.config.ts` under the `i18n.locales` array so translations can load correctly.

## Behaviour on unknown domains
If the application receives a hostname that is not present in `HOST_LOCALE_MAP`, the request falls back to English (`en-US`). During SSR a warning log is emitted describing the unknown hostname so operators can adjust the mapping. Client-side navigation continues without additional logging to avoid noise in the browser console.

## Relationship with content bundles
Locale codes correspond to the translation bundles stored under `frontend/i18n/`. Each domain listed above automatically loads the matching bundle; there is no need for query parameters or path prefixes. Manual language switching widgets should respect the hostname contract—if a different behaviour is needed, adjust the plugin first so SSR and CSR remain aligned.