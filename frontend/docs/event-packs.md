# Event Packs - Localization & Assets (Frontend)

This document explains how event packs drive both assets (parallaxes, themed visuals) and **localized copy** for the home page. Packs are resolved by name (e.g. `default`, `hold`, `sdg`, `bastille-day`) and fall back to `default` when a field is missing.

## Configuration

### Pack definitions

Event packs are configured in:

```
frontend/config/theme/event-packs.ts
```

This file contains:

- `EVENT_PACK_NAMES`: List of available pack names
- `eventPackSchedule`: Date windows for automatic pack activation
- Resolution functions (`resolveActiveEventPack`, `resolveEventPackName`)

### Adding a new pack

1. Add the pack name to `EVENT_PACK_NAMES`:

```typescript
export const EVENT_PACK_NAMES = [
  'default',
  'sdg',
  'bastille-day',
  'christmas', // New pack
  'hold',
] as const
```

2. Add the activation window in `eventPackSchedule`:

```typescript
{
  id: 'christmas',
  start: '12-15',
  end: '12-31',
  pack: 'christmas',
  description: 'Christmas period',
}
```

## I18n Structure

All pack-aware strings live under `packs.<pack>.*` at the root of locale files. The `default` branch contains the baseline values.

**Location:**

```
frontend/i18n/locales/en-US.json
frontend/i18n/locales/fr-FR.json
```

**Example structure:**

```jsonc
{
  "packs": {
    "default": {
      "hero": {
        "eyebrow": "Responsible shopping",
        "title": "Responsible choices aren't a luxury",
        "titleSubtitle": ["Buy better. Spend smarter."],
        "subtitles": [
          "Save time, stay true to your values.",
          "Shop smarter without compromise."
        ],
        "search": {
          "label": "Search for a product",
          "placeholder": "Search a product (e.g. television, smartphone...)",
          "helpers": [...]
        }
      }
    },
    "bastille-day": {
      "hero": {
        "eyebrow": "Bastille Day special",
        "title": "Celebrate conscious choices on 14 July",
        "subtitles": [
          "Shop with liberte, egalite, durabilite in mind."
        ]
      },
      "parallax": {
        "essentials": "parallax/parallax-background-bastille-essentials.svg"
      }
    }
  },
  "blog": { ... },
  "home": { ... }
}
```

### Keys you can override per pack

- `hero.title`, `hero.eyebrow`, `hero.titleSubtitle`, `hero.subtitles`
- `hero.search.*` (label, placeholder, aria, CTA, `helpersTitle`, `helpers`, partner link strings)
- `hero.context.*`
- `hero.iconAlt`, `hero.imageAlt`
- `parallax.*` (section background paths)

## Fallback Rules

1. Try `packs.<activePack>.<path>`
2. Fallback to `packs.default.<path>`
3. Optional extra fallback keys can be provided per call (legacy `home.hero.*` is kept as a safety net)

## Rendering-time Randomization

Lists (e.g. `hero.subtitles`, `hero.titleSubtitle`) are randomized **per render**. Seeds are stored in `useState('event-pack-variant-seeds')` with a deterministic key, so SSR and CSR remain consistent for a given render.

- Use `resolveStringVariant(path, { stateKey })` to pick one entry from a list
- Use `resolveList(path)` to fetch arrays (e.g. helpers)

## Consuming Pack-aware Strings

Use the composable `useEventPackI18n(packName)`:

```typescript
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
```

## Assets Configuration

### Static assets

Seasonal asset overrides are configured in:

```
frontend/config/theme/assets.ts
```

```typescript
export const seasonalThemeAssets: SeasonalThemeAssets = {
  'bastille-day': {
    light: {
      heroBackground: 'bastille-day/hero-background.svg',
      illustration: 'bastille-day/illustration-fireworks.svg',
    },
    common: {
      heroBackground: 'bastille-day/hero-background.svg',
    },
  },
}
```

### Parallax backgrounds

Parallax backgrounds can be defined in two ways:

1. **Via TypeScript config** (`assets.ts`):

```typescript
export const eventParallaxPacks = {
  light: {
    'bastille-day': {
      essentials: ['parallax/parallax-background-bastille-essentials.svg'],
      features: ['parallax/parallax-background-bastille-features.svg'],
    },
  },
}
```

2. **Via i18n** (takes precedence):

```json
{
  "packs": {
    "bastille-day": {
      "parallax": {
        "essentials": "parallax/parallax-background-bastille-essentials.svg",
        "features": "parallax/parallax-background-bastille-features.svg"
      }
    }
  }
}
```

### Asset file location

```
frontend/app/assets/themes/common/{pack-name}/
  hero-background.svg
  illustration.svg
```

## Testing a Pack

### URL parameter

Force a specific pack via URL query parameter:

```
https://nudger.fr?event=bastille-day
https://nudger.fr?event=sdg
```

**Legacy parameter (still supported):**

```
https://nudger.fr?theme=bastille-day
```

Only valid pack names from `EVENT_PACK_NAMES` are accepted. Invalid values are ignored.

### Development

```bash
pnpm dev
# Open http://localhost:3000?event=bastille-day
```

## Key Files

| File                                          | Purpose                           |
| --------------------------------------------- | --------------------------------- |
| `config/theme/event-packs.ts`                 | Pack definitions and date windows |
| `config/theme/assets.ts`                      | Asset configurations              |
| `i18n/locales/*.json`                         | Localized strings (`packs` key)   |
| `composables/useSeasonalEventPack.ts`         | Active pack resolution            |
| `composables/useEventPackI18n.ts`             | I18n string resolution            |
| `composables/useThemedParallaxBackgrounds.ts` | Parallax asset resolution         |
