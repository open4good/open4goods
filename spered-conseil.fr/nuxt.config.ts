import { defineNuxtConfig } from 'nuxt/config'

const defaultFrDomain = process.env.FR_DOMAIN ?? 'spered-conseil.fr'
const defaultEnDomain = process.env.EN_DOMAIN ?? 'spered-conseil.com'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  ssr: true,
  devtools: { enabled: false },
  modules: ['vuetify-nuxt-module', '@nuxt/content', '@nuxt/eslint'],
  css: ['vuetify/styles'],
  app: {
    head: {
      title: 'Spered Conseil',
      meta: [
        {
          name: 'viewport',
          content: 'width=device-width, initial-scale=1',
        },
      ],
    },
  },
  runtimeConfig: {
    smtp: {
      host: process.env.SMTP_HOST ?? '',
      port: Number(process.env.SMTP_PORT ?? 587),
      secure: process.env.SMTP_SECURE === 'true',
      user: process.env.SMTP_USER ?? '',
      pass: process.env.SMTP_PASS ?? '',
      from: process.env.SMTP_FROM ?? '',
      to: process.env.CONTACT_TO ?? '',
    },
    hcaptcha: {
      verifyUrl: process.env.HCAPTCHA_VERIFY_URL ?? 'https://api.hcaptcha.com/siteverify',
      secret: process.env.HCAPTCHA_SECRET ?? '',
      minScore: Number(process.env.HCAPTCHA_MIN_SCORE ?? 0),
    },
    public: {
      hcaptchaSiteKey: process.env.HCAPTCHA_SITE_KEY ?? '',
      localeDomains: {
        fr: defaultFrDomain,
        en: defaultEnDomain,
      },
    },
  },
})
