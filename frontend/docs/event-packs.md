TODO : Create FR version

# Event Packs - Localization & Assets (Frontend)

This document explains how event packs drive both assets (parallaxes, themed visuals) and **localized copy** for the home page. Packs are resolved by name (e.g. `default`, `hold`, `sdg`, `bastille-day`) based on the URL parameter `?event=` or the current date.

## Quick Start

### Testing a Pack manually

Force a specific pack via URL query parameter:

```
http://localhost:3000?event=bastille-day
http://localhost:3000?event=sdg
```

> **Note**: Only valid pack names defined in `EVENT_PACK_NAMES` are accepted.

### Adding a new pack

1.  **Define the pack**:
    Add the pack name to `EVENT_PACK_NAMES` in `frontend/config/theme/event-packs.ts`.

    ```typescript
    export const EVENT_PACK_NAMES = [
      'default',
      'sdg',
      'bastille-day',
      'christmas', // New pack
      'hold',
    ] as const
    ```

2.  **Schedule it (Optional)**:
    Add the activation window in `eventPackSchedule` in the same file.

    ```typescript
    {
      id: 'christmas',
      start: '12-15',
      end: '12-31',
      pack: 'christmas',
      description: 'Christmas period',
    }
    ```

3.  **Add content & assets**:
    Update `frontend/i18n/locales/fr-FR.json` (and other locales) with the pack configuration.

    ```json
    "packs": {
      "christmas": {
        "hero": {
          "title": "Joyeux Noël responsable",
          "subtitles": ["Offrez des cadeaux durables."]
        },
        "assets": {
          "heroBackground": "christmas/hero-background.svg"
        },
        "parallax": {
          "essentials": "parallax/christmas-essentials.svg"
        }
      }
    }
    ```
    
# A nice complete pack could contains :

> Homepage hero background image // TODO : Check src is i18n key
> Homepage hero subtitle
> Todo the "Gagne du temps. Choisis librement." title 

> TODO : Add parallax home pages paths
> TODO : Check they are well pointed and src being definied fully through i18n key 

TODO : get i18n samples containing those keys


    

## Architecture & Configuration

### Pack definitions

Event packs are configured in:
`frontend/config/theme/event-packs.ts`

This file contains:

- `EVENT_PACK_NAMES`: List of available pack names.
- `eventPackSchedule`: Date windows for automatic pack activation.
- Resolution functions (`resolveActiveEventPack`, `resolveEventPackName`).

### I18n Structure

TODO : Ensure this i18n pack overriding mechanism is generic (can be applied on any keys) and efficient / documented

All pack-aware strings and asset paths live under `packs.<pack>.*` at the root of locale files.

**Location:** `frontend/i18n/locales/fr-FR.json`

**Structure:**

```jsonc
{
  "packs": {
    "default": {
      "hero": { ... },   // Default text content
      "assets": { ... }, // Default static assets
      "parallax": { ... } // Default parallax layers
    },
    "bastille-day": {
      "hero": {
        "eyebrow": "Spécial 14 juillet",
        "title": "Célébrez des choix responsables"
      },
      "assets": {
        "heroBackground": "bastille-day/hero-background.svg"
      },
      "parallax": {
        "essentials": "parallax/parallax-background-bastille-essentials.svg"
      }
    }
  }
}
```

### Assets Configuration

Assets are now fully managed via i18n keys. This allows different assets per locale if needed, but primarily groups all pack configuration in one place.

#### Static Assets

Defined under `packs.<pack>.assets`. common keys:

- `heroBackground`
- `illustration`
- `logo`, `footerLogo`, `favicon` (mostly for `hold` pack)

#### Parallax Backgrounds

Defined under `packs.<pack>.parallax`. Keys correspond to homepage sections:

- `essentials`
- `features`
- `blog`
- `objections`
- `cta`

**Asset File Location:**
Place your SVG/image files in:
`frontend/app/assets/themes/`

You can organize them by theme or pack, e.g., `frontend/app/assets/themes/bastille-day/hero-background.svg`.
The i18n value should be the relative path from `assets/themes/`.

## Developer Guide

### Consuming Pack Strings

Use `useEventPackI18n(packName)`:

```typescript
const activePack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activePack)

// Resolves packs.<activePack>.hero.title
// Falls back to provided fallbackKeys if not found in the pack
const heroTitle = computed(() =>
  packI18n.resolveString('hero.title', { fallbackKeys: ['home.hero.title'] })
)
```

### Rendering-time Randomization

Lists (e.g. `hero.subtitles`) are randomized **per render** using a consistent seed to avoid hydration mismatches.

```typescript
const heroSubtitle = computed(() =>
  packI18n.resolveStringVariant('hero.subtitles', {
    stateKey: 'home-hero-subtitles',
    fallbackKeys: ['home.hero.subtitles'],
  })
)
```

### Asset Resolution

Use `useThemedAsset` or `useThemedParallaxBackgrounds`. These composables automatically look up the correct path in the i18n file for the active pack.

```typescript
const heroBackground = useHeroBackgroundAsset()
```
