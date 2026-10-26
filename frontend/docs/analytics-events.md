# Analytics events (Plausible)

This document tracks the analytics event catalog for the Nuxt frontend and how
to instrument new UI interactions. Use the `useAnalytics` composable, which
wraps the Plausible plugin and respects Do Not Track settings.

## Where analytics live

- **Composable**: `app/composables/useAnalytics.ts`
- **Plugin guard**: `app/plugins/plausible-guard.client.ts`
- **Nuxt config**: `nuxt.config.ts` (plausible module configuration)

## Event catalog

> **Filtrage robots sur `/contrib/` (GOU-1175)** : `/contrib/<jeton>` et `/api/contrib/<jeton>` refusent (403 `text/plain`, sans appel à front-api) les UA vides ou reconnus comme robots par `isbot`, plus les motifs de `NUXT_AFFILIATION_BLOCKED_USER_AGENTS` (sous-chaînes séparées par des virgules, insensibles à la casse, vide par défaut). Aucune page HTML n'est jamais rendue sous `/contrib/**` (erreurs en `text/plain`) et Plausible est désactivé côté client sur ce chemin.

> **Signal de navigation sur `/contrib/` (GOU-1184)** : après les règles de GOU-1175, `/contrib/<jeton>` et `/api/contrib/<jeton>` ne redirigent que si la requête porte **au moins un** signal de navigation sur le site ; sinon 403 `text/plain` "Ouvrez ce lien depuis la fiche produit sur nudger.fr" (`Cache-Control: no-store`, `X-Robots-Tag`), sans appel à front-api, donc sans `ContributionVote` ni clic transmis au réseau.
>
> 1. **`Referer`** dont l'hôte est un domaine du site (`HOST_DOMAIN_LANGUAGE_MAP` de `shared/utils/domain-language.ts` : nudger.fr, beta.nudger.fr, nudger.com...) ou l'hôte qui sert la requête. Les liens d'offre portent donc `rel="sponsored nofollow noopener"` (plus de `noreferrer`) et `referrerpolicy="same-origin"` (`AFFILIATE_LINK_REL`, `AFFILIATE_LINK_REFERRER_POLICY` dans `app/utils/_product-pricing.ts`) ; le clic sur une ligne du tableau d'offres passe par `openAffiliateLinkInNewTab` (même `rel`, même politique, ce que `window.open` ne permet pas). La 301 de `/contrib/` envoie `Referrer-Policy: no-referrer` : le réseau et le marchand ne reçoivent pas de referer nudger.
> 2. **Cookie technique `nudger_nav=1`** : first-party, `Path=/`, `SameSite=Lax`, `Secure` en HTTPS, `Max-Age` 24 h, valeur constante sans donnée personnelle. Posé côté client par `app/plugins/navigation-signal.client.ts` au chargement de l'application et à chaque navigation (`page:finish`), jamais par le SSR (le HTML reste sans `Set-Cookie`, donc cacheable). Cookie de sécurité / lutte contre la fraude au clic d'affiliation, strictement nécessaire à ce service : exempté de consentement. Il n'est lu que par `/contrib/**`.
>
> Interrupteur : `NUXT_AFFILIATION_REQUIRE_NAVIGATION_SIGNAL=false` (vrai par défaut, seule une valeur explicite `false`/`0`/`off`/`no` coupe le contrôle) rétablit le comportement de GOU-1175 sans nouveau build. Journal : un `warn` agrégé au plus par minute (nombre de refus depuis le précédent, un UA tronqué et les 6 premiers caractères du jeton en exemple), jamais d'IP ni de jeton entier.

### `affiliate-click`

Tracks clicks on affiliate redirect links (typically `/contrib/<token>`). Emit
the event from the source offer link before the browser opens the redirect;
`/contrib/<token>` is an HTTP-only redirect and does not load analytics.

Props:

- `token` (string)
- `affiliatePlatform` (string, defaults to `unknown` until exposed by backend)
- `merchantId` (string/number, optional)
- `merchantName` (string, optional)
- `merchantSlug` (string, optional)
- `placement` (string, optional)
- `productId` (string/number, optional)
- `gtin` (string/number, optional)
- `vertical` (string, optional)
- `categorySlug` (string, optional)
- `offerRank` (number, optional)
- `priceBucket` (string, optional)
- `currency` (string, optional)
- `condition` (string, optional)
- `destinationHost` (string, optional)

Do not send the full destination URL in this event. Use `destinationHost` to
keep Plausible properties low-cardinality and avoid leaking query parameters.

### `product-redirect`

Legacy affiliate redirect event (kept for compatibility).

Props:

- `token`, `placement`, `source`, `url`

### `tab-click`

Tracks UI tab selection.

Props:

- `tab` (string)
- `context` (string)
- `label` (string, optional)
- `productId` (string/number, optional)

### `category-filter-change`

Tracks filter changes on category pages.

Props:

- `categoryId`, `categorySlug`
- `action` (e.g., `manual-updated`, `subset-enabled`, `cleared`)
- `source` (e.g., `sidebar`, `drawer`, `fast-filters`, `active-filters`)
- `filtersCount`
- `filterFields` (array of mapping strings)
- `subsetIds` (array of subset ids)

### `category-sort-usage`

Tracks category sort controls.

Props:

- `categoryId`, `categorySlug`
- `action` (`exposed`, `selected`, `order-updated`)
- `selectedField`, `selectedGroup`, `sortOrder`
- `defaultField`, `primaryOptions`, `advancedOptions`, `totalOptions`

### `search`

Tracks search submissions.

Props:

- `query`, `source`, `results`

### `search-focus`

Tracks focus on search inputs to measure intent.

Props:

- `location` (page or component identifier)
- `queryLength`

### `open-data-download`

Tracks open data download actions.

Props:

- `dataset`, `method`, `href`

### `open-data-link`

Tracks clicks on the `/opendata`, `/opendata/gtin` and `/opendata/isbn` pages blocks "Explore products" and "Also available on" (emitted by `trackOpenDataOutboundClick`). Respects Do Not Track like every other event.

Props (low cardinality, no GTIN):

- `block`: `explore-products` | `mirrors`
- `target`: `category` | `product` | `datagouv` | `zenodo` | `kaggle` | `figshare`
- `page`: `opendata` | `gtin` | `isbn`
- `category`: category slug (`verticalId`, e.g. `tv`), only for `category` and `product` targets

Plausible setup: create a custom event goal named `open-data-link` (Settings > Goals > Add goal > Custom event), then register the custom properties `block`, `target`, `page` and `category` (Settings > Custom properties) to break the goal down.

### `file-download`

Tracks generic file downloads (e.g., PDF documentation).

Props:

- `fileType`, `url`, `label`, `context`

### `section-view`

Tracks section visibility (intersection observed).

Props:

- `sectionId`, `page`, `label`

### `share-resolution-*`

Tracks the web-share product resolution flow.

Events:

- `share-resolution-start`
- `share-resolution-select`
- `share-resolution-resolved`
- `share-resolution-timeout`
- `share-resolution-error`

Props vary by event and include `domainLanguage`, `originUrl`, `mode`,
`productId`, `source`, and `candidates`.

### `ai-review-methodology-click`

Tracks clicks from AI review request UI to the methodology page.

Props:

- `location`
- `labelVariant` (dialog only)

### `ai-review-request-variant-view`

Tracks the displayed AI review request CTA variant.

Props:

- `location`
- `labelVariant`

## Plausible dashboard setup

Configure these custom properties on the Plausible site to make the affiliation
goal useful in the self-hosted UI:

- `affiliatePlatform`
- `merchantSlug`
- `merchantName`
- `placement`
- `gtin`
- `vertical`
- `categorySlug`
- `offerRank`
- `priceBucket`
- `currency`
- `condition`
- `destinationHost`

Use `affiliate-click` as the canonical affiliation conversion goal. Keep
`product-redirect` only for historical comparisons while dashboards migrate.

## Implementation tips

1. **Use the composable**: `const { trackEvent, trackSearch, ... } = useAnalytics()`.
2. **Prefer semantic events**: If an interaction already maps to a helper
   (`trackAffiliateClick`, `trackFilterChange`, etc.), use it instead of
   `trackEvent` directly.
3. **Avoid personal data**: keep props to identifiers and UI context.
4. **Keep props stable**: analytics dashboards rely on consistent event names.
