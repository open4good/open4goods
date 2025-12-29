# Theme assets and seasonal packs

This page summarises how themed assets are resolved, how seasonal packs are scheduled, and how to preview a specific seasonal pack.

## Asset keys and fallbacks

- Supported asset keys are declared in `config/theme/assets.ts` (`THEME_ASSET_KEYS`).
- Resolution order for any asset key now checks:
  1. A seasonal override (if a seasonal pack is active) placed under `app/assets/themes/<theme>/<season>/<file>` or `app/assets/themes/common/<season>/<file>`.
  2. The theme-specific asset under `app/assets/themes/<theme>/<file>`.
  3. The shared `common/` asset.
  4. The fallback theme configured by `THEME_ASSETS_FALLBACK` (light by default).
- Use the composables `useThemeAsset` or `useThemedAsset` instead of importing files directly so this cascade remains consistent.

### Conventions

- **Filenames**: Use `kebab-case` for all asset filenames (e.g., `hero-background.svg`).
- **Logos**: `logo`, `footerLogo`, and `favicon` are considered **Application Identity** assets.
  - They should be defined in `config/theme/assets.ts` for `light` and `dark` themes.
  - They should **NOT** be overridden by Event Packs (do not add them to `packs.<name>.assets` in i18n files) unless a complete rebrand is intended.

### Adding seasonal overrides

- Declare the file names you intend to override in `seasonalThemeAssets` inside `config/theme/assets.ts`.
- Place the files under `app/assets/themes/<theme>/<pack>/` (or `common/<pack>/`) using the same filenames as their non-seasonal counterparts.
- Missing files automatically fall back to the non-seasonal theme/common assets thanks to the ordered resolution above.

> [!NOTE]
> A default theme kit must include not only standard page backgrounds but also the **5 parallax layers** used on the homepage (essentials, features, blog, objections, cta).

## Event packs and scheduling

- Available packs: `default`, `sdg`, `bastille-day`, and `hold` (see `EVENT_PACK_NAMES`).

- Date windows are defined in `config/theme/seasons.ts` and evaluated in UTC. If no window matches, the `default` pack is used.
- `useSeasonalEventPack` exposes the active pack to components (parallax layers, hero subtitle overrides, etc.).

## Forcing an event pack for previews

- Append `?event=<pack>` (or legacy `?theme=<pack>`) to any page URL (e.g., `?event=hold` or `?event=sdg`) to force that event pack.
- When this query parameter is present it bypasses both the calendar-based scheduling and any stored theme preference, ensuring a deterministic preview.
- Only values listed in `EVENT_PACK_NAMES` are accepted; invalid values are ignored and the schedule is used instead.
