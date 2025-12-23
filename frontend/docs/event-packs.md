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

## Checkpoints & Configuration

Here is a **minimal pack configuration** that covers all the essential customization points (Hero, Parallax, and Section Images).

```json
"packs": {
  "my-event-pack": {
    "hero": {
      // 1. Homepage Hero Title (overrides "Acheter mieux...")
      "title": "Achetez malin pour Noël",

      // 2. Homepage Hero Subtitle (overrides "Gagne du temps...")
      "subtitles": [
        "Des cadeaux durables pour toute la famille."
      ]
    },
    "assets": {
      // 3. Homepage Hero Background Image
      "heroBackground": "my-pack/hero-background.svg",

      // 4. Pain and Gain Images (Sections after Hero)
      "problemImage": "my-pack/problem.webp",
      "solutionImage": "my-pack/solution.webp"
    },
	
TODO : Add header background samples 
TODO : Create an exaustiv listing of page headers versus page route mapping
TODO : i think most /* pages headers are not using generic one
TODO : page header is not documented. Create a specific separate doc



TODO : We will complete here once investigated with major pages	
    "parallax": {
      // 5. Parallax Home Page Paths
      "essentials": "parallax/my-pack-essentials.svg",
      "features": "parallax/my-pack-features.svg",
      "blog": "parallax/my-pack-blog.svg",
      "objections": "parallax/my-pack-objections.svg",
      "cta": "parallax/my-pack-cta.svg"
    }
  }
}
```



### Full Configuration Reference

#### Pack definitions

Event packs are configured in: `frontend/config/theme/event-packs.ts`

- `EVENT_PACK_NAMES`: List of available pack names.
- `eventPackSchedule`: Date windows for automatic pack activation.

#### I18n Structure

All pack-aware strings and asset paths live under `packs.<pack>.*` at the root of locale files (`frontend/i18n/locales/fr-FR.json`).
The system prioritizes keys found in the active pack's structure.

#### Assets

Assets are managed via i18n keys resolving to paths in `frontend/app/assets/themes/`.

| Key              | Description              | Example Path in i18n  |
| :--------------- | :----------------------- | :-------------------- |
| `heroBackground` | Hero section background  | `theme/hero-bg.svg`   |
| `illustration`   | Generic illustration     | `theme/illus.svg`     |
| `problemImage`   | "Problems" section image | `theme/problem.webp`  |
| `solutionImage`  | "Solution" section image | `theme/solution.webp` |
| `logo`           | Site Logo                | `theme/logo.svg`      |

#### Parallax

#### Page Header Integration

The generic `PageHeader` component supports event pack assets for background images and expanded container options.

**Dynamic Backgrounds**

To use a pack-defined asset as a header background, use `backgroundImageAssetKey`:

```vue
<PageHeader
  title="My Page"
  background="image"
  background-image-asset-key="heroBackground"
/>
```

The key (e.g., `heroBackground`) must be defined in the pack's `assets` configuration or `themeAssets`.

**Container Options**

`PageHeader` supports a `semi-fluid` width (max 1560px), bridging the gap between standard container (1280px) and fluid (full width).

```vue
<PageHeader title="Wide Page" container="semi-fluid" />
```

Defined under `packs.<pack>.parallax`.

- `essentials`: Background for Problems/Solution stack.
- `features`: Background for Features grid.
- `blog`: Background for Blog section.
- `objections`: Background for Objections section.
- `cta`: Background for FAQ/CTA section.

## Developer Guide

### Consuming Pack Strings

Use `useEventPackI18n(packName)`:

```typescript
const activePack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activePack)

// Resolves packs.<activePack>.hero.title
const heroTitle = computed(() =>
  packI18n.resolveString('hero.title', { fallbackKeys: ['home.hero.title'] })
)
```

### Rendering-time Randomization

Lists (e.g. `hero.subtitles`) are randomized **per render** using a consistent seed.

```typescript
const heroSubtitle = computed(() =>
  packI18n.resolveStringVariant('hero.subtitles', {
    stateKey: 'home-hero-subtitles',
    fallbackKeys: ['home.hero.subtitles'],
  })
)
```

### Asset Resolution

Use `useThemeAsset` (generic) or `useThemedParallaxBackgrounds`.

```typescript
// For generic assets defined in assets.ts
const problemImage = useThemeAsset('problemImage')

// For hero background specifically
const heroBackground = useHeroBackgroundAsset()
```
