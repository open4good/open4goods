import type { NuxtConfig } from '@nuxt/schema'

export default defineNuxtConfig({
  // Enable server-side rendering
  ssr: true,
  // Disable Nuxt telemetry
  telemetry: false,
  // Source directory for the project
  srcDir: 'src',
  app: {
       baseURL: process.env.BASE_URL || '/',
  },
  // Nuxt modules
  modules: [
    '@nuxt/image',
    '@nuxtjs/i18n',
    '@nuxtjs/plausible',
    '@nuxtjs/robots',
    '@nuxtjs/seo',
    '@nuxtjs/sitemap',
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@vite-pwa/nuxt',
    '@vueuse/nuxt',
    // Only enable devtools in non-production environments
    process.env.NODE_ENV !== 'production' && '@nuxt/devtools'
  ].filter(Boolean),
  devtools: {
    enabled: process.env.NODE_ENV !== 'production'
  },

  // Additional build options
  build: {
    transpile: ['vue-demi', '@vueuse/core', '@vueuse/shared']
  },

  // Internationalization settings
  i18n: {
    locales: [
      { code: 'en', iso: 'en-US', name: 'English' },
      { code: 'fr', iso: 'fr-FR', name: 'Français' }
    ],
    defaultLocale: 'fr',
    strategy: 'prefix_except_default',
    bundle: {
      optimizeTranslationDirective: false
    }
  },
  // Runtime configuration available on both client and server
  runtimeConfig: {
    strapiToken: process.env.STRAPI_TOKEN, // private token for Strapi CMS
    public: {
      strapiUrl: process.env.STRAPI_URL || 'http://localhost:1337', // Strapi base URL
      plausibleDomain: process.env.NUXT_PUBLIC_PLAUSIBLE_DOMAIN, // Plausible domain
      siteUrl: process.env.NUXT_PUBLIC_SITE_URL // Public site URL
    }
  },
  // Progressive Web App configuration
  pwa: {
    registerType: 'autoUpdate',
    manifest: {
      name: 'Nudger',
      short_name: 'Nudger',
      start_url: '/',
      display: 'standalone',
      background_color: '#ffffff',
      theme_color: '#ffffff',
      icons: [
        {
          src: '/nudger-icon-512x512.png',
          sizes: '512x512',
          type: 'image/png'
        }
      ]
    }
  },
  // Vite configuration tweaks
  vite: {
    workerThreads: true,
    cacheDir: '.nuxt/.vite-cache',
    css: {
      devSourcemap: true
    }
  },
  // Experimental Nuxt features
  experimental: {
    inlineSSRStyles: true
  },
  // Nitro server options
  nitro: {
    logLevel: 3,
    //externals: { inline: ['vue'] },
    compatibilityDate: '2025-06-15',
    preset: process.env.NITRO_PRESET === 'github_pages' ? 'github-pages' : undefined
  }

} satisfies NuxtConfig)
