---
title: "Theme system (light/dark)"
description: "- Respect operating system preference on first visit."
tags:
  - documentation
  - vue-content
  - structural
  - frontend
owner: platform
audience: all
language: en
component: frontend
maturity: draft
security_classification: public
doc_url: /docs/apps/frontend/docs/theme-system
doc_path: apps/frontend/docs/theme-system.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Theme system (light/dark)

## Goals

- Respect operating system preference on first visit.
- Let users override the theme across the whole app.
- Keep Vuetify semantic colors and landing custom styles aligned through shared tokens.

## Source of truth

- Design tokens: `assets/style/tokens.json`
- Vuetify and CSS variable bridge: `assets/style/tokens-to-vuetify.ts`

Both `dark` and `light` themes define:

- backoffice semantic colors (`primary`, `surface`, `background`, etc.),
- landing token group (`bgBase`, `textPrimary`, `lineSubtle`, etc.).

## Runtime behavior

- Preference state is managed by `composables/useThemePreference.ts`.
- Stored key: `infera.theme.preference` persisted via Nuxt cookie (`useCookie`) so SSR and client stay aligned.
- Allowed values:
  - `system` (default)
  - `light`
  - `dark`

Resolution order:

1. Read stored preference.
2. If `system`, evaluate `prefers-color-scheme`.
3. Apply Vuetify theme + CSS variables.

## UI integration

- Global switcher component: `components/infra/InfThemeToggle.vue`
- Mounted in:
  - global app bar (`layouts/default.vue`)
  - landing header (`components/landing/LandingHeader.vue`)

## Vuetify-first guardrails (POC baseline)

- Prefer Vuetify semantic props (`color`, `variant`, `density`, `elevation`) over custom CSS colors.
- Avoid per-page color overrides (`border-color`, `background-color`, hardcoded `rgba/#hex`) unless there is no equivalent in Vuetify APIs.
- Keep custom CSS for layout/spacing/animation only.
- If a visual decision is global, apply it in `plugins/vuetify.ts` defaults instead of per-component styling.

### Allowed custom color usage

Only these cases should use custom color variables:

- Landing/brand storytelling sections that intentionally diverge from backoffice visuals.
- Explicit brand assets (logos, gradients in static brand files).
- Temporary feature flag experiments with a documented removal path.

## Validation checklist

- Open app with OS light preference and no stored choice → light theme should load.
- Open app with OS dark preference and no stored choice → dark theme should load.
- Manually select light/dark → choice persists after reload.
- Select system and change OS preference while app is open → theme updates automatically.
