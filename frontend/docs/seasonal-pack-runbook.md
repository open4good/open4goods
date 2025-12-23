# Seasonal event pack runbook

This playbook describes how to implement and verify a themed event pack (texts + assets) using the new **“bastille-day” (14 July) demo pack** as a reference. All steps assume the Nuxt frontend.

## Quick checklist

- [ ] Pick an `EVENT_PACK_NAMES` value (e.g., `bastille-day`) and add the calendar window in `config/theme/seasons.ts`.
- [ ] Declare seasonal assets in `config/theme/assets.ts` and drop the files under `app/assets/themes/<theme>/<pack>/` or `app/assets/themes/common/<pack>/`.
- [ ] Add parallax layers for each homepage section under `eventParallaxPacks` and create the matching SVGs.
- [ ] Provide i18n strings under `home.events.<pack>` (eyebrow, title, subtitle lists, search helpers, alt texts).
- [ ] Preview the pack with `?event=<pack>` to confirm overrides and fallbacks.

## Texts (home hero) – override + fallback

- [ ] Add `home.events.<pack>.hero` keys in `i18n/locales/en-US.json` and `fr-FR.json`.
  - Minimum recommended fields: `eyebrow`, `title`, `titleSubtitle` (array), `subtitles` (array), `search.label`, `search.placeholder`, `search.helpersTitle`, and `search.helpers` (icon + segments).
  - Arrays are intentionally supported: `useEventPackI18n.resolveStringVariant` picks a deterministic random entry per session, enabling lightweight rotation without code changes.
- [ ] Verify fallbacks: missing pack keys resolve to `home.events.default` and then to `home.hero` keys (see `useEventPackI18n`).
- [ ] Keep hero icon/text alts aligned with the nudge tool welcome icon semantics (`nudge-tool.wizard.welcome`) so the corner badge and hero stay consistent for screen readers.

## Assets (hero + parallax)

- [ ] Register seasonal hero/illustration assets in `seasonalThemeAssets` (e.g., `bastille-day/hero-background.svg`, `bastille-day/illustration-fireworks.svg`).
- [ ] Place SVGs in `app/assets/themes/<theme>/<pack>/` or `app/assets/themes/common/<pack>/`; the resolver cascades theme → common → fallback theme.
- [ ] For parallax, provide five files (essentials, features, blog, objections, cta) and reference them in `eventParallaxPacks`. File names should describe the section purpose (e.g., `parallax-background-bastille-features.svg`).
- [ ] Preview hero and parallax layers in both light/dark modes; all assets should keep high contrast and simple shapes.

## Scheduling and preview

- [ ] Set the UTC window in `config/theme/seasons.ts`; for the demo the `bastille-day` pack runs from **07-10** to **07-16**.
- [ ] Use `?event=bastille-day` on any page to force the pack regardless of date or stored theme.
- [ ] Confirm the active pack via `useSeasonalEventPack` and ensure fallbacks resolve when assets or strings are missing.

## Example: “bastille-day” implementation

- Seasonal assets live in `app/assets/themes/common/bastille-day/` with contrasting fireworks SVGs and parallax layers in `app/assets/themes/common/parallax/`.
- The pack overrides hero texts (English and French) under `home.events.bastille-day.hero.*` with festive subtitles and helper icons.
- The event is scheduled in July and appears when `?event=bastille-day` is set or when the calendar window is active.
