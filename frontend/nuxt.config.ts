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
  runtimeConfig: {

    // Public keys (exposed to client-side)
    public: {
      apiUrl: process.env.API_URL || 'http://localhost:8082',
    }
  },
})