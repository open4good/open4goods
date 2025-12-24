# Page Headers Documentation

This document provides a comprehensive overview of how page headers are handling across the application, mapping routes to header components and detailing the configuration of the generic `PageHeader` component.

## Header Component Mapping

Multiple header implementations exist to suit different page designs. The Generic `PageHeader` is the standard for content-heavy pages, while specialized heroes are used for Marketing/Landing pages.

| Page Route      | Component Used    | Description                                                         |
| :-------------- | :---------------- | :------------------------------------------------------------------ |
| `/partners`     | `PageHeader`      | Generic header with title/subtitle.                                 |
| `/team`         | `PageHeader`      | Generic header with title/subtitle.                                 |
| `/impact-score` | `PageHeader`      | Generic header with title/subtitle.                                 |
| `/opensource`   | `PageHeader`      | Generic header with title/subtitle.                                 |
| `/contact`      | `ContactHero`     | Specialized hero with highlights and contact channels.              |
| `/releases`     | `OpendataHero`    | Specialized hero with "latest release" badge and education card.    |
| `/opendata/*`   | `OpendataHero`    | Shared hero logic for open data pages (presumed).                   |
| `/blog`         | `TheArticles.vue` | Embedded custom hero section (HTML/CSS in component).               |
| `/` (Home)      | `HomeHero`        | Full-screen hero with nudge wizard (not listed above but distinct). |

## internal: PageHeader Component

The `PageHeader` (`app/components/shared/header/PageHeader.vue`) is the unified component for standard pages. It supports Event Packs (via `useThemeAsset`) for dynamic backgrounds.

### Usage Example

```vue
<PageHeader
  title="My Page Title"
  subtitle="A descriptive subtitle"
  background="image"
  background-image-asset-key="heroBackground"
  container="semi-fluid"
/>
```

### Configuration Options

| Prop                      | Type                                                       | Default             | Description                                       |
| :------------------------ | :--------------------------------------------------------- | :------------------ | :------------------------------------------------ |
| `title`                   | `string`                                                   | -                   | Main heading (H1).                                |
| `subtitle`                | `string`                                                   | -                   | Subheading text.                                  |
| `background`              | `'surface-variant' \| 'image' \| 'gradient' \| 'parallax'` | `'surface-variant'` | Background style.                                 |
| `backgroundImageAssetKey` | `ThemeAssetKey`                                            | -                   | Key to resolve background image from Event Packs. |
| `container`               | `'fluid' \| 'semi-fluid' \| 'lg' \| 'xl'`                  | `'lg'`              | Container width.                                  |
| `eyebrow`                 | `string`                                                   | -                   | Small uppercase label above title.                |

### Event Packs Integration

The `PageHeader` can dynamically change its background based on the active "Event Pack" (e.g., Christmas, Bastille Day).

1.  **Define Asset**: Ensure the asset key (e.g., `heroBackground`) is defined in `frontend/config/theme/assets.ts`.
2.  **Configure Pack**: In `locales/fr-FR.json` (under `packs.<packName>.assets`), map the key to a path.
3.  **Use Prop**: Pass `background-image-asset-key="heroBackground"` to `PageHeader`.

**Note**: Specialized heroes (`ContactHero`, `OpendataHero`, etc.) do **not** currently support Event Pack background switching out-of-the-box, as they rely on `HeroSurface` or custom CSS.
