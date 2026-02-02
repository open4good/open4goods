# Theme assets

This page summarises how themed assets are resolved and how to preview asset changes during development.

## Asset keys and fallbacks

- Supported asset keys are declared in `config/theme/assets.ts` (`THEME_ASSET_KEYS`).
- Resolution order for any asset key checks:
  1. The theme-specific asset under `app/assets/themes/<theme>/<file>`.
  2. The shared `common/` asset.
  3. The fallback theme configured by `THEME_ASSETS_FALLBACK` (light by default).
- Use the composables `useThemeAsset` or `useThemedAsset` instead of importing files directly so this cascade remains consistent.

### Conventions

- **Filenames**: Use `kebab-case` for all asset filenames (e.g., `hero-background.svg`).
- **Logos**: `logo`, `footerLogo`, and `favicon` are considered **Application Identity** assets.
  - They should be defined in `config/theme/assets.ts` for `light` and `dark` themes.
  - They should not be overridden by locale files unless a complete rebrand is intended.

> [!NOTE]
> A default theme kit should include standard page backgrounds plus the **5 parallax layers** used on the homepage (essentials, features, blog, objections, cta).
