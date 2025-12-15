# Theme-scoped assets

Theme-specific assets live under `app/assets/themes/<theme>/` with shared fallbacks in `app/assets/themes/common/`.

## Conventions
- Mirror filenames across themes (e.g., `logo.png`, `logo-footer.svg`, `favicon.svg`, `hero-background.svg`).
- When a themed asset is missing, the resolver falls back to `common`, then to the light theme.
- Keep assets optimised (SVG when possible) and sized for mobile-first layouts.
- Prefer descriptive filenames by purpose rather than brand (e.g., `hero-background.svg`, `illustration-generic.svg`).

## Current folders
- `light/`: source of truth for brand-ready assets.
- `dark/`: reserved for dark-specific overrides (currently falls back to light/common assets).
- `common/`: neutral visuals usable across all themes (e.g., hero backgrounds, generic illustrations).

Use the `useThemedAsset` composable to resolve the correct URL instead of importing assets directly.
