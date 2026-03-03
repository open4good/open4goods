# spered-conseil.fr

Minimal SSR Nuxt/Vuetify starter with:

- simple i18n JSON resources (`fr-FR`, `en-US`) with locale by domain,
- markdown section fragments assembled into pages with `@nuxt/content` + MDC,
- contact form using hCaptcha (`@hcaptcha/vue3-hcaptcha`),
- server route that verifies hCaptcha and sends emails via direct SMTP.

## Environment variables

```bash
FR_DOMAIN=spered-conseil.fr
EN_DOMAIN=spered-conseil.com

HCAPTCHA_SITE_KEY=
HCAPTCHA_SECRET=
HCAPTCHA_VERIFY_URL=https://api.hcaptcha.com/siteverify
HCAPTCHA_MIN_SCORE=0

SMTP_HOST=
SMTP_PORT=587
SMTP_SECURE=false
SMTP_USER=
SMTP_PASS=
SMTP_FROM=
CONTACT_TO=
```

## Run

```bash
pnpm install
pnpm dev
pnpm build
pnpm start
```
