// https://nuxt.com/docs/api/configuration/nuxt-config
import * as vuetify from 'vite-plugin-vuetify'
import { transformAssetUrls } from 'vite-plugin-vuetify'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  typescript: {
    typeCheck: true,
    tsConfig: {
      compilerOptions: {
        esModuleInterop: true
      }
    }
  },

  // Suppress Vue Router warnings for Chrome DevTools requests
  vite: {
    define: {
      __VUE_PROD_DEVTOOLS__: false,
    },
    vue: {
      template: {
        transformAssetUrls,
      },
    },
    // ✅ Hide specific warnings
    build: {
      rollupOptions: {
        external: ['virtual:#nitro-internal-virtual/storage'],
      },
    },
  },

  modules: [
    (_options, nuxt) => {
      nuxt.hooks.hook('vite:extendConfig', (config) => {
        config.plugins = config.plugins ?? []
        config.plugins.push(vuetify.default({ autoImport: true }))
      })
    },
    "@nuxtjs/i18n",
    "@nuxt/image",
    '@vueuse/nuxt',
    "@nuxt/icon",
    "@pinia/nuxt",
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
  css: [
    'assets/sass/main.sass', // Gardez seulement le fichier SASS principal
  ],
  build: {
    transpile: ['vuetify'],
  },
  nitro: {
    preset: 'static',
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
    // Private keys (only available on server-side)
    blogToken: process.env.BLOG_TOKEN,

    // Public keys (exposed to client-side)
    public: {
      blogUrl: process.env.BLOG_URL || 'https://beta.front-api.nudger.fr',
    }
  },
})
