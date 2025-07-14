// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  compatibilityDate: '2025-05-15',
  devtools: { enabled: true },

  // Suppress Vue Router warnings for Chrome DevTools requests
  vite: {
    define: {
      __VUE_PROD_DEVTOOLS__: false,
    },
  },

  modules: [
    '@nuxt/eslint',
    '@nuxt/fonts',
    '@nuxt/icon',
    '@nuxt/image',
    '@nuxt/scripts',
    '@nuxt/test-utils',
    '@nuxtjs/i18n',
    '@pinia/nuxt',
    'vuetify-nuxt-module',
  ],
  i18n: {
    defaultLocale: 'fr-FR',
    locales: [
      { code: 'fr-FR', name: 'Français' },
      { code: 'en-US', name: 'English' },
    ],
    strategy: 'prefix_except_default',
    bundle: {
      optimizeTranslationDirective: false,
    },
  },
  vuetify: {
    // https://vuetifyjs.com/en/
    // https://nuxt.vuetifyjs.com/guide/
    moduleOptions: {
      /* module specific options */
    },
    vuetifyOptions: {
      /* vuetify options */
    },
  },
})
