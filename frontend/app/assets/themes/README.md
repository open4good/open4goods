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

See [docs/theme-assets.md](../../docs/theme-assets.md) for end-to-end guidance on fallbacks, seasonal packs, and preview parameters.

## Recommended sizes & ratios
- **Hero & parallax backgrounds**: keep a single large viewBox such as `1600x900` or `1920x1080` with `preserveAspectRatio="xMidYMid slice"` and generous bleed (10–15%) so the image covers wide and tall viewports without revealing empty bands.
- **Placeholders/illustrations**: max ~`1200x900`, still exported as SVG where possible.
- Avoid fixing `height="800"` in SVG outputs; prefer `width="100%" height="100%"` and rely on the viewBox to remain crisp when parallax and overlays are applied.
