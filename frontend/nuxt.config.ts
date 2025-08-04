// https://nuxt.com/docs/api/configuration/nuxt-config

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: {
    enabled: process.env.NODE_ENV !== 'production',
    timeline: {
      enabled: true,
    },
  },

  typescript: {
    typeCheck: true,
    tsConfig: {
      compilerOptions: {
        esModuleInterop: true
      }
    }
  },

  routeRules: {
    '/blog/**': { swr: 60 }, // revalide le cache après 60s
    //'/blog/**': { isr: { expiration: 60 } } // re-génère toutes les 60s
    // pages générées une seule fois de manière statique
    //'/articles/**': { static: true },
    // zone d’admin rendue côté client
    '/admin/**': { ssr: false },
  },
  modules: [
    "vuetify-nuxt-module",
    "@nuxtjs/i18n",
    "@nuxt/image",
    '@vueuse/nuxt',
    "@nuxt/icon",
    "@pinia/nuxt",
  ],
  i18n: {
    defaultLocale: 'en-US',
    locales: [
      { code: 'fr-FR', name: 'Français' },
      { code: 'en-US', name: 'English' },
    ],
    strategy: 'prefix_except_default',
  },
  css: [
    'assets/sass/main.sass', // Gardez seulement le fichier SASS principal
  ],

  build: {
    transpile: ['vuetify'],
  },
  nitro: {
    preset: 'node-server',
    experimental: {
      wasm: true
    }
  },
  image: {
    // The screen sizes predefined by `@nuxt/image`:
    screens: {
      'xs': 320,
      'sm': 640,
      'md': 768,
      'lg': 1024,
      'xl': 1280,
      'xxl': 1536,
      '2xl': 1536
    },
  },
  ssr: true, // Disable SSR if you generate a static site
  components: [
    {
      path: '~/components',
      pathPrefix: false,
    },
  ],
  // Runtime configuration for environment variables
  // These can be overridden via a .env file
  runtimeConfig: {
    // Name of the cookie storing the JWT
    tokenCookieName: process.env.TOKEN_COOKIE_NAME || 'access_token',
    // Name of the cookie storing the refresh token
    refreshCookieName: process.env.REFRESH_COOKIE_NAME || 'refresh_token',
    // Shared token for server-to-server authentication (server-only)
    machineToken: process.env.MACHINE_TOKEN || '',

    // Public keys (exposed to client-side)
    public: {
      // Base URL of the backend API
      apiUrl: process.env.API_URL || 'http://localhost:8082',
    }
  },
})