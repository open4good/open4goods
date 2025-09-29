// https://nuxt.com/docs/api/configuration/nuxt-config

import { fileURLToPath } from 'node:url'

import xwikiSandboxPrefixerOptions from './config/postcss/xwiki-sandbox-prefixer-options.js'
import { buildI18nLocaleDomains } from './shared/utils/domain-language'
import { buildI18nPagesConfig } from './shared/utils/localized-routes'

const localeDomains = buildI18nLocaleDomains()

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  srcDir: 'app',
  dir: {
    public: 'app/public',
  },
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
    '/blog/**': { swr: 60 }, // revalidates the cache after 60s
    //'/blog/**': { isr: { expiration: 60 } } // regenerates every 60s
    // Pages generated once as static output
    //'/articles/**': { static: true },
    // Admin area rendered on the client side
    '/admin/**': { ssr: false },
  },
  modules: [
    "vuetify-nuxt-module",
    "@nuxtjs/i18n",
    "@nuxt/image",
    '@vueuse/nuxt',
    "@nuxt/icon",
    "@pinia/nuxt",
    'nuxt-mcp'
  ],

  vuetify: {
      vuetifyOptions: {
        theme: {
          defaultTheme: 'light',
          themes: {
            light: {
              colors: {
                primary: '#1976D2',
                secondary: '#424242',
                accent: '#82B1FF',
                error: '#FF5252',
                info: '#2196F3',
                success: '#4CAF50',
                warning: '#FFC107',
                red: '#F44336',
                green: '#4CAF50',
                'team-hero-gradient-start': '#1976D2',
                'team-hero-gradient-end': '#43A047',
                'team-card-highlight': '#2196F3',
                'team-card-accent': '#4CAF50',
                'team-callout-surface-start': '#ECF8EF',
                'team-callout-surface-end': '#E3F2FD',
                'team-callout-accent': '#1976D2',
                'team-callout-accent-text': '#152E49',
                'team-section-light-start': '#FFFFFF',
                'team-section-light-end': '#F5FAFF',
                'team-section-muted-start': '#F8FAFC',
                'team-section-muted-end': '#EEF4FA',
              },
            },
            dark: {
              colors: {
                primary: '#90CAF9',
                secondary: '#EEEEEE',
                accent: '#82B1FF',
                error: '#FF867C',
                info: '#64B5F6',
                success: '#81C784',
                warning: '#FFD54F',
                red: '#EF9A9A',
                green: '#81C784',
                'team-hero-gradient-start': '#1E3A8A',
                'team-hero-gradient-end': '#166534',
                'team-card-highlight': '#38BDF8',
                'team-card-accent': '#22C55E',
                'team-callout-surface-start': '#1E293B',
                'team-callout-surface-end': '#0F172A',
                'team-callout-accent': '#2563EB',
                'team-callout-accent-text': '#E2E8F0',
                'team-section-light-start': '#1E293B',
                'team-section-light-end': '#111827',
                'team-section-muted-start': '#111827',
                'team-section-muted-end': '#0B1220',
              },
            },
          },
        },
      },
    },
  i18n: {
    defaultLocale: 'en-US',
    langDir: '../i18n/locales',
    locales: [
      { code: 'fr-FR', name: 'Français', file: 'fr-FR.ts', ...(localeDomains['fr-FR'] ?? {}) },
      { code: 'en-US', name: 'English', file: 'en-US.ts', ...(localeDomains['en-US'] ?? {}) },
    ],
    strategy: 'no_prefix',
    detectBrowserLanguage: false,
    customRoutes: 'config',
    differentDomains: true,
    pages: buildI18nPagesConfig(),
    vueI18n: './i18n.config.ts',
  },
  css: [
    'vuetify/styles',
    '~/assets/sass/main.sass',
  ],

  postcss: {
    plugins: {
      'postcss-prefix-selector': {
        includeFiles: [
          /\/assets\/css\/bootstrap\.css$/i,
        ],
        ...xwikiSandboxPrefixerOptions,
      },
    },
  },

  build: {
    transpile: ['vuetify'],
  },
  nitro: {
    preset: 'node-server',
    publicAssets: [{ dir: 'app/public' }],
    experimental: {
      wasm: true
    }
  },
  image: {
    dir: fileURLToPath(new URL('./app/public', import.meta.url)),
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
    apiUrl: process.env.API_URL || 'http://localhost:8082',

    // Public keys (exposed to the client side)
    public: {
      // Base URL of the backend API
      // Roles allowed to edit content blocks (defaults to backend role names)
      editRoles: (process.env.EDITOR_ROLES || 'ROLE_SITEEDITOR,XWIKIADMINGROUP').split(','),
    }
  },
})