# Product Page Audit & Fix Plan - UI consistency + SEO/affiliate maximization

> **Status tracking**: check the boxes as steps complete. Each work package (WP)
> is an independent, coherent PR. Execute WPs in order unless stated otherwise.
> Every WP ends with explicit verification; do not mark a WP done if any
> verification step fails.
>
> **Audience**: coding agents. Read `AGENTS.md` (repo root) and
> `frontend/AGENTS.md` before touching code. Baseline is the **current working
> tree** (an icon migration is in progress, uncommitted - do not revert it).

---

## 1. Context

Audit of the nudger.fr product page (June 2026 data, audited 2026-07-02) with two goals:

1. **UI**: eliminate inconsistencies and "bad feeling" moments.
2. **SEO**: maximize clicks on affiliation links.

Evidence base:
- Code exploration of the working tree.
- Production HTML of the top product page (`https://nudger.fr/climatiseurs/8431312260509-climatisation-midea-mmcs12hrn8qrd0`).
- Google Search Console (June 2026): product pages rank pos 5-8 on model-number
  queries (`samsung rb38c655ds9`, `bp1125h`, `whirlpool whk26493x5e`...) with ~0-2% CTR.
- Local `pnpm dev` renders + Playwright screenshots (desktop 1440×900, mobile 390×844).

Decisions already made by the product owner (do NOT re-litigate):
- SERP titles keep the ImpactScore **always** (brand transparency), even when bad.
- Add editorial `Review` JSON-LD (Nudger verdict → `reviewRating`) for SERP stars.
- The plan verifies `pnpm build && pnpm preview`; the production deploy itself is
  done by the owner (out of agent scope).
- Mobile above-the-fold: **bold reorder approved** - compact gallery, price/CTA
  in the first viewport.

### Key architecture facts

| Thing | Where |
|---|---|
| Product page component | `frontend/app/components/pages/ProductPage.vue` (~2700 lines) |
| Route (catch-all) | `frontend/app/pages/[...slug].vue` → `matchProductRouteFromSegments` in `frontend/shared/utils/_product-route.ts` |
| Section components | `frontend/app/components/product/**` |
| SERP/social meta builders | `seoMetaBase` / `socialMetaBase` computed in `ProductPage.vue` (~lines 2230-2360), templates in `frontend/i18n/locales/fr-FR.json` + `en-US.json` under `product.meta.*` |
| JSON-LD builder | `frontend/app/utils/product-jsonld.ts` (`buildProductJsonLdGraph`), injected in `ProductPage.vue` (~line 2393) |
| Canonical + hreflang | `canonicalPath`/`alternateProductLinks` in `ProductPage.vue` (~lines 944-985) |
| Affiliate redirect | `/contrib/{token}` → `frontend/server/api/contrib/[token].ts` → front-api `AffiliationController` (301) |
| Price format utils | `frontend/app/utils/_product-pricing.ts` (`formatPrice`, `formatBestPrice`, `formatOffersCount`) |
| Visual tests | `frontend/tests/visual/*.spec.ts`, `frontend/playwright.config.ts` (chromium, dev server :3000) |
| Unit tests | Vitest, colocated `*.spec.ts` next to components |
| Local dev | `cd frontend && pnpm dev` - `.env` already points `API_URL` to `https://front-api.nudger.fr`. If :3000 is taken, Nuxt falls back to :3001 - check the log. |

### SSR census script (used by several WPs)

Save once as `frontend/scripts/ssr-census.mjs` in **WP0 step 3** and reuse everywhere.
It fetches a product page and reports: `<title>`, meta description, h1-h4 list,
JSON-LD parse result, `<section id>` list + whether each has text content,
`/contrib/` anchors with their `rel`, and hreflang links. See WP0 for the spec.

Reference product URLs for testing (path part is identical locally):
- `/climatiseurs/8431312260509-climatisation-midea-mmcs12hrn8qrd0` (sparse data: no detailed attrs, 2 offers)
- `/televiseurs/8806096715659-televiseur-lg-55qned7eb3c-2026` (rich data)

---

## 2. Audit findings (evidence, keep for reference)

### SEO
- **S1 - CRITICAL**: production HTML contains empty `<section>` shells for
  `impact`, `vigilance`, `caracteristiques`, `alternatives`, `cycle-de-vie` - no
  content, no h2s. The local working tree SSRs all of it. Production runs a stale
  build → attributes, impact details and alternative-product internal links are
  invisible to Google.
- **S2**: SERP title `MIDEA MMCS-12HRN8-QRD0 : ImpactScore : 3.4/20` - no
  category keyword, no commercial phrasing.
- **S3**: meta description duplicates brand-model: "Comparez MIDEA
  MMCS-12HRN8-QRD0 (MIDEA MMCS-12HRN8-QRD0)..." - templates interpolate both
  `{productName}` and `{brandModel}`, which are usually identical.
- **S4**: no `Review`/`AggregateRating` in JSON-LD → not eligible for SERP stars.
- **S5**: affiliate `rel` inconsistent: `ProductPriceRows.vue` has
  `nofollow noopener noreferrer`; `ProductPriceSection.vue` only `nofollow`;
  `ProductHeroPricingPanel.vue` main link has **no rel**;
  `ProductStickyPriceBanner.vue` only `noopener noreferrer`. `rel="sponsored"`
  appears **nowhere**.
- **S6**: hreflang `en-US` points to `nudger.com/<french-path>` → 301 to
  `http://www.nudger.com/...` → **404**. Emitted on every product page.
- **S7**: JSON-LD `BreadcrumbList` brand item ("MIDEA") uses the category URL
  `/climatiseurs` - duplicate of the category item URL.
- **S8**: duplicate `id="prix"` - the page-level `<section id="prix">` wrapper in
  `ProductPage.vue` AND an inner `<section id="prix">` inside
  `ProductPriceSection.vue`. Invalid HTML.
- **S9**: JSON-LD `Offer.availability` hardcoded `InStock`; `priceValidUntil`
  synthetic (now + 10 days).
- **S11**: `h1` is bare `MIDEA MMCS-12HRN8-QRD0` - no category keyword, while
  GSC queries are mostly `<category-word> <brand> <model>`.
- Verified working: legacy `/{gtin}-{slug}` URLs 301 to canonical; canonical tag;
  BreadcrumbList; Product JSON-LD basics (gtin13, brand, model, AggregateOffer).

### UI (from screenshots)
- **U1**: impact-score card: "FAIB" chip overlaps "Classement : Mieux que 95 %";
  "Min : 0 / Max : 5" shown beside a `/20` score (two scales side by side).
  Appears both in the hero score card and the Impact section card.
- **U2**: verdict panel chips render **empty pale circles** (icon/gauge missing);
  2 of 3 chips read "Données insuffisantes" - a negative wall above the fold.
- **U3**: PWA toast "Mode hors ligne prêt" overlaps the offers card on load.
- **U4**: four price formats coexist: `999 €` (hero), `999,00 €` (rows),
  `433.2 €` (alternatives - dot decimal, wrong locale), `1 890 €`.
- **U5**: merchant name truncated ("castor...") in the hero pricing panel despite
  available space.
- **U6**: buy CTA styles differ per location: `variant="text"` (price rows),
  `flat` (hero), `elevated` (MicroPrice); condition colors are
  `success`/`warning` in PriceSection but `primary`/`secondary` in MicroPrice.
  No prominent explicit "Voir l'offre" CTA above the fold.
- **U7**: section headers hand-rolled per section; "Caractéristiques techniques"
  is center-aligned while all other sections are left-aligned;
  `ProductLifeTimeline` has **no h2 at all**; `ProductVerdictPanel` uses Vuetify
  utility classes instead of the BEM `__title` convention.
- **U8**: emoji heading "😘 Moins cher, et meilleur pour la planète ?" in
  `ProductAlternatives.vue`.
- **U9**: vigilance "Informations contradictoires" card lists **raw attribute
  keys** (`COLOUR`, `NOISE_LEVEL`); "Fiche d'identité" shows raw internal values
  (`8431312260509_CAFR` as an "autre appellation").
- **U10**: offers table renders pagination controls ("Éléments par page 5,
  1-2 de 2") for 2 offers.
- **U11**: empty-state noise: attributes search field + grid/table toggle
  rendered above "Aucune caractéristique détaillée disponible"; big empty
  price-history block when there is no history.
- **U13**: icon migration half-done: 11 components migrated to `@mdi/js`
  imports, 13 still use string `"mdi-..."` literals (list in WP7).
- **U16**: mobile: the gallery fills the entire first viewport; title, verdict
  and price CTA are below the fold.

---

## 3. Work packages

Legend per step: **[edit]** code change · **[verify]** must pass · **[owner]** human action.

---

### WP0 - Ship SSR'd content to production (S1, S8) - CRITICAL, do first

**Goal**: prove the production build SSRs all sections, fix the duplicate id,
hand off to the owner for deploy.

- [ ] **0.1 [edit]** Fix duplicate `id="prix"`:
  - `frontend/app/components/pages/ProductPage.vue` keeps its
    `<section :id="sectionIds.price">` wrapper (id `prix`, used by
    `ProductSummaryNavigation` scroll-spy).
  - `frontend/app/components/product/ProductPriceSection.vue` contains an inner
    `<section id="prix">` - remove the `id` from the inner element (keep the
    element). Grep first: `grep -n 'id="prix"' frontend/app/components -r`.
    Check nothing targets the inner one (`grep -rn "'#prix'\|\"#prix\"" frontend/app`).
- [ ] **0.2 [edit]** Create `frontend/scripts/ssr-census.mjs`: a Node script,
  no new dependencies (use global `fetch` + regex/`JSON.parse`), usage:
  `node scripts/ssr-census.mjs <url>`. It must print:
  1. `<title>` text and meta description content;
  2. all h1-h4 with text (tag + text, in order);
  3. every `<section id=...>` and a boolean "has visible text content" (strip tags,
     >40 chars of text before the next section starts);
  4. each JSON-LD block: parse OK/fail + list of `@type`s found;
  5. all `<a>` whose href contains `/contrib/`: their `rel` and `target`;
  6. all `link rel=alternate` hreflang entries (hreflang + href);
  7. exit code 1 if: no h1, or any JSON-LD parse fails, or any `/contrib/` anchor
     lacks `sponsored` in rel (this last check may fail until WP1 lands - pass
     `--no-rel-check` flag to skip it in WP0).
- [ ] **0.3 [verify]** Dev-mode census passes:
  `cd frontend && pnpm dev` (note the actual port in the log), then
  `node scripts/ssr-census.mjs http://localhost:<port>/climatiseurs/8431312260509-climatisation-midea-mmcs12hrn8qrd0 --no-rel-check`.
  Expect: h1 + h2s for Impact Score / Prix et évolution / Points de vigilance /
  Caractéristiques techniques; sections `impact`, `vigilance`, `caracteristiques`,
  `alternatives` all "has content = true"; exactly ONE `id="prix"` occurrence in
  the raw HTML (`curl -s <url> | grep -c 'id="prix"'` → 1).
- [ ] **0.4 [verify]** **Production-mode** census passes (this is the point of WP0 -
  dev mode proving it is NOT sufficient):
  `cd frontend && pnpm build && pnpm preview` then run the census against the
  preview port on BOTH reference URLs. All sections must have content in the
  served HTML. If async sections come back empty in preview mode, that is the
  production bug reproducing - investigate `defineAsyncComponent` hydration in
  `ProductPage.vue` (sections are `defineAsyncComponent(() => import(...))`);
  fix so SSR renders them (e.g. static imports for SSR-critical sections or
  Nuxt lazy hydration), then re-verify. Document what was found in the PR.
- [ ] **0.5 [verify]** `pnpm test` green, Playwright visual specs green
  (`pnpm exec playwright test`).
- [ ] **0.6 [owner]** Deploy frontend to production. Afterward re-run the census
  against `https://nudger.fr/...` - sections must have content in prod HTML.

**Acceptance**: census green in preview mode on both URLs; single `id="prix"`;
tests green.

---

### WP1 - Affiliate link hygiene (S5) - small, high confidence

**Goal**: every outbound offer link is `/contrib/{token}` when a token exists and
carries `rel="sponsored nofollow noopener noreferrer"` + `target="_blank"`.

- [ ] **1.1 [edit]** In `frontend/app/utils/_product-pricing.ts` add:
  ```ts
  /** rel for outbound affiliate/offer links - Google requires `sponsored` for paid links. */
  export const AFFILIATE_LINK_REL = 'sponsored nofollow noopener noreferrer'
  ```
  Also add (or move here, if an equivalent exists - search
  `grep -rn "affiliationToken" frontend/app/components/product` and factor the
  repeated `/contrib/${token}` construction) a helper:
  ```ts
  export const resolveOfferHref = (offer: { affiliationToken?: string | null; url?: string | null }) =>
    offer.affiliationToken ? `/contrib/${offer.affiliationToken}` : offer.url ?? undefined
  ```
- [ ] **1.2 [edit]** Apply both in every offer-link render:
  - `frontend/app/components/product/ProductPriceRows.vue` (currently
    `nofollow noopener noreferrer`, lines ~19-20, 94-95; local href logic ~157-164)
  - `frontend/app/components/product/ProductPriceSection.vue` (row buttons,
    best-new/best-occasion card wrappers, price-history CTA - currently bare
    `nofollow`; local `resolveOfferLink` ~974-979 → replace with the shared helper)
  - `frontend/app/components/product/ProductHeroPricingPanel.vue` (main panel
    link has NO rel today; merchant-row buttons)
  - `frontend/app/components/product/ProductHeroPricing.vue`: replace the
    `window.open(panel.merchant.url, ...)` path (~line 444) with a real rendered
    `<a>`/`:href` element so rel applies and middle-click/SEO semantics work.
  - `frontend/app/components/product/ProductStickyPriceBanner.vue` (currently
    `noopener noreferrer` only) - note: if it only scrolls to the offers section
    (emits `scroll-to-offers`) leave it; only outbound hrefs need the rel.
  - `frontend/app/components/product/ProductMicroPrice.vue`: uses raw
    `bestOffer.url` - switch to `resolveOfferHref` so the token/tracking is not lost.
- [ ] **1.3 [edit]** Unit tests: in the colocated specs
  (`ProductPriceRows.spec.ts`, `ProductPriceSection.spec.ts`, hero pricing spec...)
  assert rendered offer links have `rel === AFFILIATE_LINK_REL` and href starts
  with `/contrib/` when a token is present.
- [ ] **1.4 [verify]** `pnpm test` green. Census (`--no-rel-check` removed) green:
  every `/contrib/` anchor now reports `sponsored nofollow noopener noreferrer`.

**Acceptance**: census rel-check passes on both reference URLs; no remaining
`window.open` for merchant URLs; no raw `offer.url` when a token exists.

---

### WP2 - SERP title/description/h1 rework (S2, S3, S11)

**Goal**: commercial, keyword-bearing titles that keep the score, deduped
descriptions, category keyword in h1.

- [ ] **2.1 [edit]** `frontend/i18n/locales/fr-FR.json` - rework
  `product.meta.serp.title.*` (keep the `full`/`compact`/`minimal` +
  `withImpact`/`withoutImpact`/`noCategory` structure so `seoMetaBase`'s
  length-fallback logic keeps working). Score stays in ALL `withImpact` variants
  (owner decision). Target shape (adjust for the 60-char budget logic in
  `seoMetaBase`):
  - `withImpact.full`: `"{productName} : meilleur prix, caractéristiques et avis | ImpactScore {score}/20"`
  - `withImpact.compact`: `"{productName} : meilleur prix | ImpactScore {score}/20"`
  - `withImpact.minimal`: `"{productName} : ImpactScore {score}/20"`
  - `withoutImpact.*`: same minus the score segment.
  - Note `{productName}` already contains brand+model; the **category keyword**
    comes from the h1/vertical work in 2.3 and the `withImpactVertical`
    description variants - check whether `seoMetaBase` has access to
    `verticalTitle` for titles too; if yes, prefer
    `"{verticalTitle} {productName} : meilleur prix | ImpactScore {score}/20"`
    for the full variant.
  - Mirror changes in `en-US.json` (English copy).
- [ ] **2.2 [edit]** Fix the brand-model duplicate (S3): in `seoMetaBase` /
  description building (`ProductPage.vue` ~2230-2290), when
  `brandModel === productName` (case/whitespace-insensitive compare), pass a
  variant WITHOUT the parenthesized `{brandModel}`. Cleanest: add i18n
  description keys without the `({brandModel})` segment and select them when the
  values are equal - do not string-replace at runtime.
- [ ] **2.3 [edit]** h1 category keyword (S11): in `ProductHero.vue` (h1 via
  `<ProductDesignation title-tag="h1">`, line ~62), prefix the vertical singular
  label when available from `categoryDetail` (e.g. "Climatiseur MIDEA
  MMCS-12HRN8-QRD0"). Check what the category DTO exposes
  (`grep -n "singular\|verticalTitle\|title" frontend/app/components/pages/ProductPage.vue | head`)
  - front-api VerticalConfig has i18n names; use the existing prop passed to the
  hero/breadcrumbs rather than adding an API field. Visible title and `og:title`
  may diverge (og keeps the social template) - that is fine.
- [ ] **2.4 [verify]** Census on both URLs: title matches the new template,
  description has no duplicated brand-model, h1 contains the category word.
  `pnpm test` green (several specs assert meta/title - update them).

**Acceptance**: for the Midea page, title ≈
`Climatiseur MIDEA MMCS-12HRN8-QRD0 : meilleur prix | ImpactScore 3.4/20` (or
the closest fitting variant), description without `(MIDEA MMCS-12HRN8-QRD0)`.

---

### WP3 - JSON-LD enrichment (S4, S7, S9)

**Goal**: SERP star eligibility via an editorial Review; fix breadcrumb brand
item; honest offer fields. All in `frontend/app/utils/product-jsonld.ts`
(+ its colocated spec).

- [ ] **3.1 [edit]** Add on the Product node (only when an impact score exists):
  ```json
  "review": {
    "@type": "Review",
    "author": { "@type": "Organization", "name": "Nudger", "url": "https://nudger.fr" },
    "reviewRating": { "@type": "Rating", "ratingValue": 0.9, "bestRating": 5, "worstRating": 0 },
    "name": "ImpactScore Nudger",
    "reviewBody": "<the verdict/punchline text already available to the builder>"
  }
  ```
  `ratingValue` = impact score (on 20) ÷ 4, rounded to 1 decimal. Use the score
  source that already feeds `additionalProperty` "Nudger Impact Score".
- [ ] **3.2 [edit]** Breadcrumb brand item (S7): in the breadcrumb assembly
  (`jsonLdBreadcrumbs` in `ProductPage.vue` ~2128 feeding the builder), the brand
  crumb currently carries the category URL. If a brand-filtered category URL
  exists (check how the visible breadcrumb builds its brand link in
  `productBreadcrumbs` ~line 844 - if it also points to the category URL, there
  is no distinct brand URL), then **drop the brand ListItem from the JSON-LD
  breadcrumb only** (visible breadcrumb unchanged). No two ListItems may share
  an `item` URL.
- [ ] **3.3 [edit]** Offers honesty (S9): inspect the offer DTO
  (`frontend/shared/api-client` or wherever `product.offers` types live -
  `grep -rn "availability\|inStock" frontend/shared frontend/app/utils/product-jsonld.ts`).
  If a real availability/stock field exists, map it; if not, keep `InStock` and
  add a code comment stating the DTO has no availability data. For
  `priceValidUntil`, derive from the offer's last-update timestamp (+N days) if
  present; otherwise keep the synthetic value.
- [ ] **3.4 [edit]** Extend the builder's colocated spec: review node present
  with correctly normalized rating; no duplicate breadcrumb item URLs.
- [ ] **3.5 [verify]** `pnpm test` green. Census: JSON-LD parses, `@type`s
  include `Product` + `Review` (nested). Manually validate one page's JSON-LD at
  https://validator.schema.org (paste the block) and with Google's Rich Results
  test if accessible.

**Acceptance**: valid JSON-LD with Review; zero duplicate breadcrumb URLs;
spec coverage for both.

---

### WP4 - Fix broken hreflang (S6)

**Goal**: stop emitting alternates that 404.

- [ ] **4.1 [edit]** `alternateProductLinks` (`ProductPage.vue` ~963-985)
  currently builds `en-US` from `PRIMARY_LOCALE_HOSTS` + the **French** path.
  `nudger.com/<french-path>` → 404. Determine whether the product API exposes a
  per-locale slug (`grep -rn "fullSlug" frontend/shared frontend/app | grep -i "en\|locale"`).
  - If an English slug exists → emit it.
  - If not (expected) → emit only `fr-FR` + `x-default` for product pages and
    delete the en-US branch (owner prefers deletion over compat).
- [ ] **4.2 [edit]** Update/add a unit test asserting product pages emit exactly
  fr-FR + x-default (or the en slug case if it exists).
- [ ] **4.3 [verify]** Census: hreflang list correct; `curl -sI` each emitted
  alternate URL → 200.

---

### WP5 - Above-the-fold conversion pass (U1, U2, U5, U6-partial, U16)

**Goal**: the first viewport sells: clear score, honest verdict, visible price
CTA - desktop and mobile. This WP changes layout: screenshot before/after.

- [ ] **5.1 [edit]** Verdict panel (`ProductVerdictPanel.vue`,
  `ProductVerdictDimensionChip.vue`):
  - Fix the empty pale circles: the chip's leading visual (icon or mini-gauge)
    renders blank - reproduce locally (Midea URL), inspect why (likely a
    missing/renamed icon from the icons.ts deletion or an undefined score value)
    and fix.
  - Dimensions with no data ("Données insuffisantes"): render them compact +
    neutral gray (not bordered red/orange) OR omit them when ≥2 dimensions lack
    data, keeping a single muted line "Données partielles pour ce produit".
    Choose the option that reads calmer in the screenshot; document choice in PR.
- [ ] **5.2 [edit]** Score card overlap (U1): component is the score card in the
  hero (and reused in the impact section - find it:
  `grep -rln "Classement\|Mieux que" frontend/app/components`). Fix the
  "FAIB"/"Classement : Mieux que 95 %" collision (flex wrap / min-width), and
  remove the `Min : 0 / Max : 5` block - the /20 scale is the only one shown.
- [ ] **5.3 [edit]** Hero pricing panel (`ProductHeroPricingPanel.vue`):
  - Merchant name: remove the aggressive truncation (allow wrap or larger
    max-width; "castorama.fr" must display fully at 1440px).
  - Make the primary action an explicit CTA button: full-width
    `v-btn color="primary" variant="flat" size="large"` labeled via new i18n key
    (fr: `Voir l'offre - {price}` or `Voir chez {merchant}`), href per WP1
    helper + rel. The whole-panel-clickable behavior can stay, but the button is
    the visual anchor.
  - Price display through `formatPrice` (WP6 does the global sweep; do this
    panel now since we touch it).
- [ ] **5.4 [edit]** Mobile fold reorder (U16, owner-approved bold change) in
  `ProductHero.vue`: on `xs/sm`, order = breadcrumb → h1 → price CTA block →
  compact gallery (height-capped carousel ~240px with the thumbnails row
  removed or collapsed) → verdict panel. Desktop (md+) layout unchanged. Prefer
  CSS order/`v-row` reordering over duplicating markup; if the pricing panel
  must appear in two spots, extract, don't copy.
- [ ] **5.5 [edit]** Playwright: add `frontend/tests/visual/product-fold.spec.ts`
  taking desktop (1440×900) and mobile (390×844) viewport screenshots of the
  reference TV product URL (stable product), asserting `toHaveScreenshot` - this
  creates the baseline for future changes. Also assert: mobile first-viewport
  contains the price CTA (locator visible without scrolling).
- [ ] **5.6 [verify]** `pnpm test`, `pnpm exec playwright test` green; manual
  screenshot review (attach to PR): no overlap in score card, verdict reads
  neutral, merchant name full, CTA prominent, mobile fold shows price.

---

### WP6 - Consistency system pass (U4, U7, U8, U9, U10, U11, U6-rest)

**Goal**: one heading system, one price format, one CTA grammar, no raw
internals, no empty-state noise. Mostly mechanical; keep each step reviewable.

- [ ] **6.1 [edit]** Create
  `frontend/app/components/product/ProductSectionHeader.vue`: props
  `{ title: string; subtitle?: string; headingId?: string }`, renders
  `<header><h2 class="product-section__title">...</h2><p class="product-section__subtitle">...</p></header>`,
  left-aligned, spacing tokens consistent with the existing `__header` blocks.
  Replace the hand-rolled headers in: `ProductPriceSection.vue`,
  `ProductVigilanceSection.vue`, `ProductAttributesSection.vue`,
  `ProductImpactSection.vue`, `ProductDocumentationSection.vue`,
  `ProductAlternatives.vue`, and **add** it to `ProductLifeTimeline.vue`
  (currently no h2 - new i18n title key, e.g. fr "Cycle de vie"). Kill the
  center-alignment of "Caractéristiques techniques". `ProductVerdictPanel.vue`
  keeps an h2 but switch its classes to the same BEM title class.
- [ ] **6.2 [edit]** Price format sweep (U4): every displayed price goes through
  `formatPrice`/`formatBestPrice` from `frontend/app/utils/_product-pricing.ts`.
  Known offenders: `ProductAlternatives.vue` local `formatCurrency` (~line 389,
  produces `433.2 €`) - delete it; `ProductMicroPrice.vue`;
  `ProductHeroPricing(.Panel).vue`; check `ProductStickyPriceBanner.vue` and
  `ProductCard.vue`. Add a vitest asserting fr-FR output (`1 890,00 €` style -
  match whatever `formatPrice` produces; the point is ONE format everywhere).
- [ ] **6.3 [edit]** CTA + condition color grammar (U6 rest): in
  `_product-pricing.ts` export `NEW_CONDITION_COLOR = 'success'`,
  `OCCASION_CONDITION_COLOR = 'secondary'` (pick the pair already dominant in
  `ProductPriceSection.vue`; just make it single-sourced) and use them in
  `ProductPriceSection.vue`, `ProductMicroPrice.vue`, `ProductPriceRows.vue`,
  sticky banner. Align buy-button style: `color="primary" variant="flat"`
  everywhere a buy/offer CTA renders (rows may use `size="small"`).
- [ ] **6.4 [edit]** `ProductAlternatives.vue`: replace the "😘 ..." heading with
  sober copy via i18n (fr e.g. "Alternatives : moins cher, meilleur impact"),
  through `ProductSectionHeader`.
- [ ] **6.5 [edit]** Offers table (U10): in `ProductPriceSection.vue`, hide the
  v-data-table footer when `items.length <= itemsPerPage`
  (`hide-default-footer` or slot override).
- [ ] **6.6 [edit]** Empty states (U11): `ProductAttributesSection.vue` - when
  there are no detailed attributes, render nothing for the "Caractéristiques
  détaillées" sub-block (no search field, no toggle, no empty message if the
  synthesis block above already shows content). Price history
  (`ProductPriceSection.vue`): when history has < 2 points, collapse the card to
  a single muted line instead of the large empty sparkline block.
- [ ] **6.7 [edit]** Raw internals (U9):
  - Vigilance contradictory-attributes card: find where the attribute names come
    from (`grep -rn "COLOUR\|conflicts\|contradict" frontend/app/components/product frontend/app/composables`)
    and resolve them through the same label translation the attributes section
    uses; fallback = lowercase + capitalize + underscores→spaces.
  - Identity card "autres appellations": filter values matching
    `/^\d{8,}_[A-Z]/` (GTIN-prefixed internal codes like `8431312260509_CAFR`)
    out of the displayed list.
- [ ] **6.8 [verify]** `pnpm test`, Playwright (visual baselines from WP5 will
  need regeneration - regenerate deliberately, review the diff images, commit).
  Census still green.

---

### WP7 - Finish the icon migration (U13)

**Goal**: zero string `"mdi-..."` literals under `frontend/app/components/product`.

- [ ] **7.1 [edit]** Migrate to `import { mdiX } from '@mdi/js'` +
  `:icon="mdiX"` (pattern identical to the 11 already-migrated components):
  `ProductHeroPricingPanel.vue`, `ProductHeroPricing.vue`,
  `ProductHeroGallery.vue`, `ProductHeroInlineGallery.vue`,
  `ProductMicroPrice.vue`, `ProductPriceRows.vue`,
  `ProductStickyPriceBanner.vue`, `ProductVerdictPanel.vue`,
  `ManufacturingChain.vue`, `attributes/ProductAttributesDetailCard.vue`,
  `impact/ProductImpactEcoScoreCard.vue`,
  `impact/ProductImpactSubscoreExplanation.vue`,
  `impact/ProductImpactSubscoreRating.vue`.
  Sweep check: `grep -rn '"mdi-\|'"'"'mdi-' frontend/app/components/product` → 0 hits.
- [ ] **7.2 [edit]** Check `@iconify-json/mdi` usage
  (`grep -rn "i-mdi\|mdi:" frontend/app frontend/nuxt.config.ts | grep -v node_modules`);
  if only the Nuxt Icon local-collection discovery uses it and no component
  references `mdi:` names, remove it from `package.json` (`pnpm remove @iconify-json/mdi`).
  If anything uses it, leave it and note in the PR.
- [ ] **7.3 [verify]** `pnpm test`;
  `pnpm exec playwright test tests/visual/product-icons.spec.ts` green (asserts
  inline SVG, no webfont, no `@mdi/font` stylesheet).

---

### WP8 - PWA toast collision (U3)

**Goal**: the offline-ready toast never overlaps content at load.

- [ ] **8.1 [edit]** `frontend/app/components/pwa/PwaOfflineNotice.vue`
  (mounted in `frontend/app/layouts/default.vue`): show at most once per session
  (sessionStorage flag), delay until `requestIdleCallback`/2s after mount,
  render as a standard `v-snackbar` bottom-left with a short timeout, and make
  sure it never captures clicks outside itself. Update
  `PwaOfflineNotice.spec.ts`.
- [ ] **8.2 [verify]** `pnpm test`; manual check on the Midea URL: toast no
  longer overlaps the offers card (screenshot).

---

## 4. Global verification & measurement

After each WP: `pnpm test` + census script on both reference URLs.
After WP5/WP6 (layout): Playwright visual + manual screenshot review.
Before the final merge: `pnpm build && pnpm preview` + census (production-mode
guard - this is what WP0 protects).

Post-deploy measurement (owner / follow-up):
- GSC: product-page CTR (filter `pageFilter contains /` product pattern) vs the
  June 2026 baseline (model-number queries at pos 5-8, CTR ~0-2%); Review-snippet
  status under Search Console → Enhancements after WP3.
- Plausible: `affiliate-click` and `product-redirect` events (tracked in
  `frontend/app/composables/useAnalytics.ts`). Note: the local Plausible MCP has
  an invalid/missing `PLAUSIBLE_API_KEY` - fix that separately to query the data.

## 5. Out of scope / known non-issues

- Legacy `/{gtin}-{slug}` 301s verified working in production - no action.
- Canonical tag, BreadcrumbList presence, Product/AggregateOffer basics - already correct.
- Production deploy itself - owner action (WP0.6).
- Category/entry-path SEO, snippets for blog pages (the GSC quick-wins list is
  dominated by the TV-size blog post - separate initiative).
