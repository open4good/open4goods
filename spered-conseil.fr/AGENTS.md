# spered-conseil.fr – Lightweight Agent Guide

This guide applies to everything under `spered-conseil.fr/`.

## Stack

- Nuxt 3 (SSR Node server)
- Vue 3 + TypeScript (`<script setup lang="ts">`)
- Vuetify 3
- `@nuxt/content` (Markdown + MDC)
- `vue-i18n` with JSON locale files

## Architecture boundaries

- Keep this project minimal and independent.
- Do not import code from the existing `frontend` app except by manual adaptation.
- Keep browser code free of secrets.
- Any secret-based integration (SMTP credentials, hCaptcha secret) must stay server-side.

## Content model

- Page content must come from markdown fragments in `content/`.
- Fragments are assembled into full pages by locale and page key.

## I18n

- All static UI text must come from `i18n/locales/*.json`.
- Supported locales: `fr-FR` and `en-US`.
- Locale routing strategy is domain-based (no URL locale prefix).

## Contact security

- hCaptcha token is required for contact submission.
- Server route must validate token against hCaptcha verify API.
- Mail delivery is direct SMTP and fully env-configurable.

## Validation before commit

- `pnpm build`
- `pnpm lint`
