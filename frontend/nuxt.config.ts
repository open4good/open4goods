import { defineNuxtConfig } from 'nuxt/config'

import { fileURLToPath } from 'node:url'
import { readFileSync } from 'node:fs'

import type { ManifestOptions } from 'vite-plugin-pwa'

import VueDevTools from 'vite-plugin-vue-devtools'
import { VueMcp } from 'vite-plugin-vue-mcp'

import {
  DEFAULT_NUXT_LOCALE,
  buildI18nLocaleDomains,
} from './shared/utils/domain-language'
import {
  APP_ROUTES_SITEMAP_KEY,
  SITEMAP_PATH_PREFIX,
} from './shared/utils/sitemap-config'
import {
  LOCALIZED_ROUTE_PATHS,
  LOCALIZED_WIKI_PATHS,
  buildI18nPagesConfig,
} from './shared/utils/localized-routes'
import { collectStaticPageRouteNames } from './scripts/static-main-page-routes'
import { vuetifyPalettes } from './config/theme/palettes'
import { icons } from './app/config/icons'

const APP_PAGES_DIR = fileURLToPath(new URL('./app/pages', import.meta.url))
const manifestFile = new URL('./app/public/site.webmanifest', import.meta.url)
const nudgerManifest = JSON.parse(
  readFileSync(manifestFile, 'utf-8')
) as ManifestOptions
const PRECACHE_EXTENSIONS = [
  'js',
  'css',
  'html',
  'ico',
  'png',
  'svg',
  'webp',
  'jpg',
  'jpeg',
  'json',
  'txt',
  'mp4',
  'webm',
  'webmanifest',
  'woff2',
]
const PRECACHE_PATTERN = `**/*.{${PRECACHE_EXTENSIONS.join(',')}}`
const WORKBOX_GLOB_IGNORES = ['videos/**/*', '**/_payload.json']
const PLAUSIBLE_DOMAIN = process.env.PLAUSIBLE_DOMAIN || 'nudger.fr'
const PLAUSIBLE_API_HOST =
  process.env.PLAUSIBLE_API_HOST || 'https://plausible.nudger.fr'
const PLAUSIBLE_ENABLED = process.env.NODE_ENV === 'production'
const PLAUSIBLE_IGNORED_HOSTNAMES = ['localhost', '127.0.0.1']
const HOTJAR_ENABLED =
  process.env.HOTJAR_ENABLED !== undefined
    ? process.env.HOTJAR_ENABLED === 'true'
    : process.env.NODE_ENV === 'production'
const HOTJAR_SITE_ID = Number(process.env.HOTJAR_SITE_ID ?? 0)
const HOTJAR_SNIPPET_VERSION = Number(process.env.HOTJAR_SNIPPET_VERSION ?? 6)
const navigationOfflineFallbackPlugin = {
  handlerDidError: async () => {
    const offlineResponse = await globalThis.caches?.match('/offline')
    return offlineResponse ?? Response.error()
  },
}

const runtimeCaching = [
  {
    urlPattern: ({ request }) => request.destination === 'image',
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'nudger-images',
      expiration: {
        maxEntries: 120,
        maxAgeSeconds: 60 * 60 * 24 * 30,
      },
    },
  },
  {
    urlPattern: ({ url }) =>
      [
        'https://beta.front-api.nudger.fr',
        'https://front-api.nudger.fr',
      ].includes(url.origin),
    handler: 'NetworkFirst',
    options: {
      cacheName: 'nudger-api',
      networkTimeoutSeconds: 5,
      cacheableResponse: { statuses: [0, 200] },
      expiration: {
        maxEntries: 80,
        maxAgeSeconds: 60 * 5,
      },
    },
  },
  {
    urlPattern: ({ url }) =>
      ['https://fonts.googleapis.com'].includes(url.origin),
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'nudger-font-styles',
      expiration: {
        maxEntries: 20,
        maxAgeSeconds: 60 * 60 * 24 * 30,
      },
    },
  },
  {
    urlPattern: ({ url }) => ['https://fonts.gstatic.com'].includes(url.origin),
    handler: 'CacheFirst',
    options: {
      cacheName: 'nudger-font-files',
      cacheableResponse: { statuses: [0, 200] },
      expiration: {
        maxEntries: 40,
        maxAgeSeconds: 60 * 60 * 24 * 365,
      },
    },
  },
  {
    urlPattern: ({ url }) =>
      ['https://cdn.jsdelivr.net', 'https://unpkg.com'].includes(url.origin),
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'nudger-static-cdn',
      cacheableResponse: { statuses: [0, 200] },
      expiration: {
        maxEntries: 60,
        maxAgeSeconds: 60 * 60 * 24 * 7,
      },
    },
  },
  {
    urlPattern: ({ request }) => request.mode === 'navigate',
    handler: 'NetworkOnly',
    options: {
      cacheName: 'nudger-pages',
      plugins: [navigationOfflineFallbackPlugin],
    },
  },
]

const STATIC_MAIN_PAGE_ROUTE_NAMES = Array.from(
  new Set([
    ...collectStaticPageRouteNames(APP_PAGES_DIR, { rootDir: APP_PAGES_DIR }),
    ...Object.keys(LOCALIZED_ROUTE_PATHS),
  ])
).sort((a, b) => a.localeCompare(b))

process.env.NUXT_STATIC_MAIN_PAGE_ROUTES = JSON.stringify(
  STATIC_MAIN_PAGE_ROUTE_NAMES
)

const localeDomains = buildI18nLocaleDomains()

const VENDOR_CHUNK_MATCHERS = [
  {
    pattern: /[\\/]node_modules[\\/]echarts[\\/]/,
    chunkName: 'vendor-echarts',
  },
  {
    pattern: /[\\/]node_modules[\\/]vue-echarts[\\/]/,
    chunkName: 'vendor-echarts',
  },
  {
    pattern: /[\\/]node_modules[\\/]date-fns[\\/]/,
    chunkName: 'vendor-date-fns',
  },
  {
    pattern: /[\\/]node_modules[\\/]@hcaptcha[\\/]vue3-hcaptcha[\\/]/,
    chunkName: 'vendor-hcaptcha',
  },
  {
    pattern: /[\\/]node_modules[\\/]highlight\.js[\\/]/,
    chunkName: 'vendor-highlight',
  },
  {
    pattern: /[\\/]node_modules[\\/]markdown-it[\\/]/,
    chunkName: 'vendor-markdown',
  },
  {
    pattern: /[\\/]node_modules[\\/]vuetify[\\/]/,
    chunkName: 'vendor-vuetify',
  },
  { pattern: /[\\/]node_modules[\\/]@vueuse[\\/]/, chunkName: 'vendor-vueuse' },
  {
    pattern: /[\\/]node_modules[\\/]@vue-pdf-viewer[\\/]/,
    chunkName: 'vendor-pdf-viewer',
  },
  {
    pattern: /[\\/]node_modules[\\/]vue-pdf-embed[\\/]/,
    chunkName: 'vendor-pdf-viewer',
  },
  {
    pattern: /[\\/]node_modules[\\/]vue3-picture-swipe[\\/]/,
    chunkName: 'vendor-gallery',
  },
]

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  srcDir: 'app',
  dir: {
    public: 'app/public',
  },
  app: {
    head: {
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        {
          rel: 'icon',
          type: 'image/png',
          href: '/pwa-assets/icons/android/android-launchericon-96-96.png',
          sizes: '96x96',
        },
        { rel: 'shortcut icon', href: '/favicon.ico' },
        {
          rel: 'apple-touch-icon',
          sizes: '180x180',
          href: '/pwa-assets/icons/ios/180.png',
        },
        { rel: 'manifest', href: '/site.webmanifest' },
      ],
      meta: [
        {
          name: 'theme-color',
          content: '#00DE9F',
          media: '(prefers-color-scheme: light)',
        },
        {
          name: 'theme-color',
          content: '#121212',
          media: '(prefers-color-scheme: dark)',
        },
        { name: 'mobile-web-app-capable', content: 'yes' },
        {
          name: 'apple-mobile-web-app-status-bar-style',
          content: 'black-translucent',
        },
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

  experimental: {
    payloadExtraction: true,
  },

  typescript: {
    typeCheck: true,
    tsConfig: {
      compilerOptions: {
        esModuleInterop: true,
        typeRoots: ['types', '../types', './node_modules/@types'],
        paths: {
          'vue3-picture-swipe': ['../types/vue3-picture-swipe'],
        },
      },
    },
  },

  routeRules: {
    '/blog/**': { swr: 60 }, // revalidates the cache after 60s
    //'/blog/**': { isr: { expiration: 60 } } // regenerates every 60s
    // Pages generated once as static output
    //'/articles/**': { static: true },
    // Admin area rendered on the client side
    '/admin/**': { ssr: false },
    '/offline': { prerender: true },
    '/ecoscore': { redirect: { to: '/impact-score', statusCode: 301 } },
    '/eco-score': { redirect: { to: '/impact-score', statusCode: 301 } },
  },
  modules: [
    'vuetify-nuxt-module',
    '@nuxt/content',
    '@nuxtjs/i18n',
    '@nuxt/image',
    '@vueuse/nuxt',
    '@nuxt/icon',
    '@pinia/nuxt',
    '@nuxtjs/sitemap',
    '@vite-pwa/nuxt',
    '@nuxtjs/plausible',
    '@nuxtjs/device',
  ],

  vueuse: {
    ssrHandlers: true,
  },

  device: {
    refreshOnResize: false,
  },

  plausible: {
    domain: PLAUSIBLE_DOMAIN,
    apiHost: PLAUSIBLE_API_HOST,
    enabled: PLAUSIBLE_ENABLED,
    autoPageviews: true,
    autoOutboundTracking: true,
    ignoredHostnames: PLAUSIBLE_IGNORED_HOSTNAMES,
  },

  pwa: {
    registerType: 'autoUpdate',
    manifest: nudgerManifest,
    manifestFilename: 'site.webmanifest',
    includeAssets: [
      'pwa-assets/icons/**/*.png',
      'pwa-assets/screenshots/*.png',
      'resources/**/*',
    ],
    client: {
      installPrompt: true,
    },
    devOptions: {
      enabled: true,
      suppressWarnings: true,
    },
    workbox: {
      cleanupOutdatedCaches: true,
      globPatterns: [PRECACHE_PATTERN],
      globIgnores: WORKBOX_GLOB_IGNORES,
      maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
      runtimeCaching,
    },
  },

  // Themes palettes are now defined in /frontend/config/theme/palettes.ts
  vuetify: {
    icons: {
      defaultSet: 'mdi',
      sets: ['mdi'],
      aliases: icons,
    },
    vuetifyOptions: {
      theme: {
        defaultTheme: 'light',
        themes: {
          light: {
            colors: vuetifyPalettes.nudger,
          },
          dark: {
            colors: vuetifyPalettes.dark,
          },
        },
      },
    },
  },
  i18n: {
    defaultLocale: 'fr-FR',
    langDir: '../i18n/locales',
    locales: [
      {
        code: 'fr-FR',
        name: 'Français',
        file: 'fr-FR.ts',
        ...(localeDomains['fr-FR'] ?? {}),
      },
      {
        code: 'en-US',
        name: 'English',
        file: 'en-US.ts',
        ...(localeDomains['en-US'] ?? {}),
      },
    ],
    strategy: 'no_prefix',
    detectBrowserLanguage: false,
    customRoutes: 'config',
    differentDomains: true,
    pages: buildI18nPagesConfig(),
    vueI18n: './i18n.config.ts',
  },
  css: ['vuetify/styles', '~/assets/sass/main.sass'],

  sitemap: {
    credits: false,
    autoLastmod: false,
    sitemapsPathPrefix: SITEMAP_PATH_PREFIX,
    zeroRuntime: true,
    sitemaps: {
      [APP_ROUTES_SITEMAP_KEY]: {
        sitemapName: `${APP_ROUTES_SITEMAP_KEY}.xml`,
        includeAppSources: true,
        exclude: ['/offline', '/share/callback'],
      },
    },
  },

  vite: {
    plugins: [
      ...(process.env.NODE_ENV !== 'production'
        ? [
            VueMcp(),
            VueDevTools({
              launchEditor: 'code', // Antigravity is VS Code-based
            }),
          ]
        : []),
    ],
    build: {
      chunkSizeWarningLimit: 3000,
      rollupOptions: {
        output: {
          manualChunks(id) {
            const matchedChunk = VENDOR_CHUNK_MATCHERS.find(({ pattern }) =>
              pattern.test(id)
            )
            return matchedChunk?.chunkName
          },
        },
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
      wasm: true,
    },
  },
  image: {
    dir: fileURLToPath(new URL('./app/public', import.meta.url)),
    // The screen sizes predefined by `@nuxt/image`:
    screens: {
      xs: 320,
      sm: 640,
      md: 768,
      lg: 1024,
      xl: 1280,
      xxl: 1536,
      '2xl': 1536,
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
    // Shared token for server-to-server authentication (server-only)
    machineToken: process.env.MACHINE_TOKEN || 'CHANGE_ME_SHARED_TOKEN',
    apiUrl: process.env.API_URL || 'http://localhost:8082',
    staticMainPageRoutes: STATIC_MAIN_PAGE_ROUTE_NAMES,
    sitemapLocalFiles: {
      fr: [
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/fr/blog-posts.xml`,
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/fr/category-pages.xml`,
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/fr/product-pages.xml`,
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/fr/wiki-pages.xml`,
      ],
      en: [
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/default/blog-posts.xml`,
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/default/category-pages.xml`,
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/default/product-pages.xml`,
        `${process.env.SITEMAP_BASE_PATH || '/opt/open4goods/sitemap'}/default/wiki-pages.xml`,
      ],
    },

    // Public keys (exposed to the client side)
    public: {
      apiBase: process.env.PUBLIC_API_URL || 'http://localhost:8082',
      // Name of the cookie storing the JWT
      tokenCookieName: process.env.TOKEN_COOKIE_NAME || 'access_token',
      // Name of the cookie storing the refresh token
      refreshCookieName: process.env.REFRESH_COOKIE_NAME || 'refresh_token',

      // Base URL of the backend API
      // Roles allowed to edit content blocks (defaults to backend role names)
      editRoles: (
        process.env.EDITOR_ROLES || 'ROLE_SITEEDITOR,XWIKIADMINGROUP'
      ).split(','),
      hcaptchaSiteKey: process.env.HCAPTCHA_SITE_KEY || '',
      staticServer: process.env.STATIC_SERVER || 'https://static.nudger.fr',
      plausible: {
        domain: PLAUSIBLE_DOMAIN,
        apiHost: PLAUSIBLE_API_HOST,
        enabled: PLAUSIBLE_ENABLED,
        autoPageviews: true,
        autoOutboundTracking: true,
        ignoredHostnames: PLAUSIBLE_IGNORED_HOSTNAMES,
      },
      hotjar: {
        enabled: HOTJAR_ENABLED,
        siteId: HOTJAR_SITE_ID,
        snippetVersion: HOTJAR_SNIPPET_VERSION,
      },
    },
  },
  hooks: {
    'pages:extend'(pages) {
      const normalizePath = (file?: string) => file?.split('\\').join('/')

      const gtinRedirectPage = pages.find(page =>
        normalizePath(page.file)?.endsWith('/app/pages/[gtin].vue')
      )

      if (gtinRedirectPage) {
        gtinRedirectPage.path = '/:gtin(\\d{6,})'
      }

      const wikiSourcePage = pages.find(page =>
        normalizePath(page.file)?.includes('/app/pages/xwiki-fullpage.vue')
      )

      if (!wikiSourcePage) {
        return
      }

      Object.entries(LOCALIZED_WIKI_PATHS).forEach(([routeName, locales]) => {
        if (
          routeName === wikiSourcePage.name ||
          pages.some(page => page.name === routeName)
        ) {
          return
        }

        const clonedPage = structuredClone(wikiSourcePage)
        const defaultLocalePath =
          locales[DEFAULT_NUXT_LOCALE]?.path ?? `/${routeName}`
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
        clonedPage.alias = Array.from(
          new Set([...existingAliases, ...localizedAliases])
        )

        pages.push(clonedPage)
      })
    },
  },
})
