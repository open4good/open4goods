# Event packs – localization & assets (frontend)

This document explains how event packs drive both assets (parallaxes, themed visuals) and **localized copy** for the home page. Packs are resolved by name (e.g. `default`, `christmas`, `sdg`) and fall back to `default` when a field is missing.

## I18n structure

All pack-aware strings live under `home.events.<pack>.*`. The `default` branch contains the baseline values. Example (trimmed):

```jsonc
{
  "home": {
    "events": {
      "default": {
        "hero": {
          "eyebrow": "Responsible shopping",
          "title": "Responsible choices aren’t a luxury",
          "titleSubtitle": ["Buy better. Spend smarter."],
          "subtitles": [
            "Save time, stay true to your values. Compare effortlessly, choose freely.",
            "Shop smarter without compromise. Nudger balances planet and price.",
          ],
          "search": {
            "label": "Search for a product",
            "placeholder": "Search a product (e.g. television, smartphone…)",
            "ariaLabel": "Search for a responsible product",
            "cta": "NUDGER",
            "helpersTitle": "Shop with intention. Compare for impact.",
            "helpers": [
              {
                "icon": "🌿",
                "segments": [
                  {
                    "text": "A unique ecological assessment",
                    "to": "/impact-score",
                  },
                ],
              },
            ],
            "partnerLinkLabel": "{formattedCount} partner | {formattedCount} partners",
            "partnerLinkFallback": "our partners",
          },
          "context": {
            "ariaLabel": "Hero context card summarising Nudger’s promise",
          },
          "iconAlt": "Nudger PWA launcher icon",
          "imageAlt": "Illustration of the Nudger comparison experience...",
        },
      },
      "christmas": {
        "hero": {
          "titleSubtitle": [
            "Find gifts that respect your values and your budget.",
          ],
          "subtitles": [
            "Give with intention this season. Compare prices and impact in one place.",
          ],
        },
      },
    },
  },
}
```

Keys you can override per pack (non-exhaustive):

- `hero.title`, `hero.eyebrow`, `hero.titleSubtitle`, `hero.subtitles`
- `hero.search.*` (label, placeholder, aria, CTA, `helpersTitle`, `helpers`, partner link strings)
- `hero.context.*`
- `hero.iconAlt`, `hero.imageAlt`

## Fallback rules

1. Try `home.events.<activePack>.<path>`.
2. Fallback to `home.events.default.<path>`.
3. Optional extra fallback keys can be provided per call (legacy `home.hero.*` is kept as a safety net).

## Rendering-time randomisation

Lists (e.g. `hero.subtitles`, `hero.titleSubtitle`) are randomised **per render**. Seeds are stored in `useState('event-pack-variant-seeds')` with a deterministic key, so SSR and CSR remain consistent for a given render.

- Use `resolveStringVariant(path, { stateKey })` to pick one entry from a list.
- Use `resolveList(path)` to fetch arrays (e.g. helpers).

## Consuming pack-aware strings

Use the composable `useEventPackI18n(packName)`:

```ts
const activePack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activePack)

const heroTitle = computed(() =>
  packI18n.resolveString('hero.title', { fallbackKeys: ['home.hero.title'] })
)

const heroSubtitle = computed(() =>
  packI18n.resolveStringVariant('hero.subtitles', {
    stateKey: 'home-hero-subtitles',
    fallbackKeys: ['home.hero.subtitles'],
  })
)

const helpers = computed(() =>
  packI18n.resolveList('hero.search.helpers', {
    fallbackKeys: ['home.hero.search.helpers'],
  })
)
```

Functions provided:

- `resolveString(path, { fallbackKeys? })` → string | undefined
- `resolveStringVariant(path, { stateKey?, randomize?, fallbackKeys? })` → string | undefined (handles lists)
- `resolveList<T>(path, { fallbackKeys? })` → `T[]`

## Alignment with assets

Event pack names mirror the asset packs in `config/theme/assets.ts` (`eventParallaxPacks`). Select the active pack via `useSeasonalEventPack`, then pass it to both the asset resolver (`useThemedParallaxBackgrounds`) and the i18n resolver (`useEventPackI18n`) to keep visuals and text in sync.
