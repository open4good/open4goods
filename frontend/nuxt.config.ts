// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from 'nuxt/config'
import { fileURLToPath } from 'node:url'

import xwikiSandboxPrefixerOptions from './config/postcss/xwiki-sandbox-prefixer-options.js'
import { DEFAULT_NUXT_LOCALE, buildI18nLocaleDomains } from './shared/utils/domain-language'
import { APP_ROUTES_SITEMAP_KEY, SITEMAP_PATH_PREFIX } from './shared/utils/sitemap-config'
import { LOCALIZED_ROUTE_PATHS, LOCALIZED_WIKI_PATHS, buildI18nPagesConfig } from './shared/utils/localized-routes'
import { collectStaticPageRouteNames } from './scripts/static-main-page-routes'

const APP_PAGES_DIR = fileURLToPath(new URL('./app/pages', import.meta.url))

const STATIC_MAIN_PAGE_ROUTE_NAMES = Array.from(
  new Set([
    ...collectStaticPageRouteNames(APP_PAGES_DIR, { rootDir: APP_PAGES_DIR }),
    ...Object.keys(LOCALIZED_ROUTE_PATHS),
  ]),
).sort((a, b) => a.localeCompare(b))

process.env.NUXT_STATIC_MAIN_PAGE_ROUTES = JSON.stringify(STATIC_MAIN_PAGE_ROUTE_NAMES)

const localeDomains = buildI18nLocaleDomains()

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  srcDir: 'app',
  dir: {
    public: 'app/public',
  },
  app: {
    head: {
      link: [
        { rel: 'icon', type: 'image/png', href: '/favicon-96x96.png', sizes: '96x96' },
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'shortcut icon', href: '/favicon.ico' },
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
        { rel: 'manifest', href: '/site.webmanifest' },
      ],
    },
  },
  site: {
    url: 'https://nudger.fr',
    name: 'Nudger',
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
        esModuleInterop: true,
        typeRoots: ['types', '../types', './node_modules/@types'],
        paths: {
          'vue3-picture-swipe': ['../types/vue3-picture-swipe']
        }
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
    'nuxt-mcp',
    '@nuxtjs/sitemap',
  ],

  vueuse: {
    ssrHandlers: true,
  },

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
                'hero-gradient-start': '#1976D2',
                'hero-gradient-mid': '#1976D2',
                'hero-gradient-end': '#43A047',
                'hero-overlay-strong': '#FFFFFF',
                'hero-overlay-soft': '#FFFFFF',
                'hero-pill-on-dark': '#FFFFFF',
                'surface-default': '#FFFFFF',
                'surface-muted': '#F8FAFC',
                'surface-alt': '#EEF4FA',
                'surface-glass': '#F4F7FA',
                'surface-glass-strong': '#FBFCFD',
                'surface-primary-050': '#F4F8FD',
                'surface-primary-080': '#EDF4FB',
                'surface-primary-100': '#E8F1FB',
                'surface-primary-120': '#E3EFFA',
                'surface-ice-050': '#EEF4FA',
                'surface-ice-100': '#F5FAFF',
                'surface-muted-contrast': '#F5F5F5',
                'border-primary-strong': '#C6DDF4',
                'shadow-primary-600': '#1976D2',
                'text-neutral-strong': '#101828',
                'text-neutral-secondary': '#475467',
                'text-neutral-soft': '#667085',
                'text-on-accent': '#152E49',
                'accent-primary-highlight': '#2196F3',
                'accent-supporting': '#4CAF50',
                'impact-score-active': '#4CAF50',
                'impact-score-inactive': '#CBD5E1',
                'chart-range-bar': '#1D4ED8',
                'surface-callout-start': '#ECF8EF',
                'surface-callout-end': '#E3F2FD',
                'accent-callout': '#1976D2',
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
                'hero-gradient-start': '#1E3A8A',
                'hero-gradient-mid': '#1D4ED8',
                'hero-gradient-end': '#166534',
                'hero-overlay-strong': '#FFFFFF',
                'hero-overlay-soft': '#FFFFFF',
                'hero-pill-on-dark': '#FFFFFF',
                'surface-default': '#000000',
                'surface-muted': '#111827',
                'surface-alt': '#1E293B',
                'surface-glass': '#1E293B',
                'surface-glass-strong': '#111827',
                'surface-primary-050': '#0B1220',
                'surface-primary-080': '#13213B',
                'surface-primary-100': '#1B2A44',
                'surface-primary-120': '#22304C',
                'surface-ice-050': '#152238',
                'surface-ice-100': '#0F172A',
                'surface-muted-contrast': '#1F2937',
                'border-primary-strong': '#1E40AF',
                'shadow-primary-600': '#3B82F6',
                'text-neutral-strong': '#F8FAFC',
                'text-neutral-secondary': '#CBD5F5',
                'text-neutral-soft': '#94A3B8',
                'text-on-accent': '#E2E8F0',
                'accent-primary-highlight': '#38BDF8',
                'accent-supporting': '#22C55E',
                'impact-score-active': '#22C55E',
                'impact-score-inactive': '#475569',
                'chart-range-bar': '#60A5FA',
                'surface-callout-start': '#1E293B',
                'surface-callout-end': '#0F172A',
                'accent-callout': '#2563EB',
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

  sitemap: {
    credits: false,
    autoLastmod: false,
    sitemapsPathPrefix: SITEMAP_PATH_PREFIX,
    sitemaps: {
      [APP_ROUTES_SITEMAP_KEY]: {
        sitemapName: `${APP_ROUTES_SITEMAP_KEY}.xml`,
        includeAppSources: true,
      },
    },
  },

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
    staticMainPageRoutes: STATIC_MAIN_PAGE_ROUTE_NAMES,

    // Public keys (exposed to the client side)
    public: {
      // Base URL of the backend API
      // Roles allowed to edit content blocks (defaults to backend role names)
      editRoles: (process.env.EDITOR_ROLES || 'ROLE_SITEEDITOR,XWIKIADMINGROUP').split(','),
      hcaptchaSiteKey: process.env.HCAPTCHA_SITE_KEY || '',
      staticServer: process.env.STATIC_SERVER || 'https://nudger.fr',
    }
  },
  hooks: {
    'pages:extend'(pages) {
      const normalizePath = (file?: string) => file?.split('\\').join('/')

      const gtinRedirectPage = pages.find((page) =>
        normalizePath(page.file)?.endsWith('/app/pages/[gtin].vue')
      )

      if (gtinRedirectPage) {
        gtinRedirectPage.path = '/:gtin(\\d{6,})'
      }

      const wikiSourcePage = pages.find((page) =>
        normalizePath(page.file)?.includes('/app/pages/xwiki-fullpage.vue')
      )

      if (!wikiSourcePage) {
        return
      }

      Object.entries(LOCALIZED_WIKI_PATHS).forEach(([routeName, locales]) => {
        if (routeName === wikiSourcePage.name || pages.some(page => page.name === routeName)) {
          return
        }

        const clonedPage = structuredClone(wikiSourcePage)
        const defaultLocalePath = locales[DEFAULT_NUXT_LOCALE]?.path ?? `/${routeName}`
        const existingAliases = Array.isArray(wikiSourcePage.alias)
          ? wikiSourcePage.alias
          : wikiSourcePage.alias
            ? [wikiSourcePage.alias]
            : []
        const localizedAliases = Object.values(locales)
          .map(config => config.path)
          .filter(path => path && path !== defaultLocalePath)

        clonedPage.name = routeName
        clonedPage.path = defaultLocalePath
        clonedPage.alias = Array.from(new Set([...existingAliases, ...localizedAliases]))

        pages.push(clonedPage)
      })
    },
  },
})