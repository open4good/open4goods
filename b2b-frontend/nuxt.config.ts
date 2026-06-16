import vuetify from 'vite-plugin-vuetify'

export default defineNuxtConfig({
  modules: ['@nuxtjs/i18n', '@nuxtjs/seo', '@nuxt/eslint', '@nuxtjs/plausible', '@nuxt/content'],
  components: [
    {
      path: '~/components',
      pathPrefix: true,
    },
    {
      path: '~/components/infra',
      pathPrefix: false,
    },
  ],
  css: ['vuetify/styles', '@mdi/font/css/materialdesignicons.css', 'md-editor-v3/lib/style.css', '~/assets/styles/main.scss', '~/assets/styles/landing.scss'],
  app: {
    head: {
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/icon-32x32.png' },
        { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/icon-16x16.png' },
        { rel: 'apple-touch-icon', href: '/apple-touch-icon.png' },
        { rel: 'manifest', href: '/site.webmanifest' },
        { rel: 'preconnect', href: 'https://maps.googleapis.com' },
        { rel: 'preconnect', href: 'https://maps.gstatic.com', crossorigin: '' }
      ],
      meta: [
        { name: 'theme-color', content: '#FFFFFF' }
      ]
    }
  },
  build: {
    transpile: ['vuetify', 'vue-countup-v3']
  },
  runtimeConfig: {
    backendOpenApiUrl: 'http://localhost:8087/v3/api-docs',
    githubToken: '',
    githubDocsBranch: 'main',
    oidcGoogleIssuer: 'https://accounts.google.com',
    oidcGoogleScopes: 'openid profile email',
    oidcGoogleClientId: '',
    oidcGoogleClientSecret: '',
    oidcGoogleRedirectUri: '',
    oidcGoogleTokenEndpoint: 'https://oauth2.googleapis.com/token',
    oidcMicrosoftIssuer: 'https://login.microsoftonline.com/common/oauth2/v2.0',
    oidcMicrosoftTokenEndpoint: 'https://login.microsoftonline.com/common/oauth2/v2.0/token',
    oidcMicrosoftScopes: 'openid profile email',
    oidcMicrosoftClientId: '',
    oidcMicrosoftClientSecret: '',
    oidcMicrosoftRedirectUri: '',
    oidcGithubIssuer: 'https://github.com/login/oauth',
    oidcGithubTokenEndpoint: 'https://github.com/login/oauth/access_token',
    oidcGithubScopes: 'read:user user:email',
    oidcGithubClientId: '',
    oidcGithubClientSecret: '',
    oidcGithubRedirectUri: '',
    oidcAppleIssuer: 'https://appleid.apple.com',
    oidcAppleTokenEndpoint: 'https://appleid.apple.com/auth/token',
    oidcAppleScopes: 'openid name email',
    oidcAppleClientId: '',
    oidcAppleClientSecret: '',
    oidcAppleRedirectUri: '',
    backendProxyTarget: '',
    routerProxyTarget: '',
    remoteProxyAllowMutations: false,
    public: {
      backendBaseUrl: 'http://localhost:8087',
      routerBaseUrl: 'http://localhost:8087',
      googleMapsApiKey: '',
      googleOidcClientId: '',
      plausibleEnabled: false,
      plausibleDomain: '',
      plausibleApiHost: 'https://plausible.io'
    }
  },
  plausible: {
    enabled: process.env.NUXT_PUBLIC_PLAUSIBLE_ENABLED === 'true',
    domain: process.env.NUXT_PUBLIC_PLAUSIBLE_DOMAIN || '',
    apiHost: process.env.NUXT_PUBLIC_PLAUSIBLE_API_HOST || 'https://plausible.io'
  },
  i18n: {
    baseUrl: 'https://product-data-api.com',
    strategy: 'prefix_except_default',
    defaultLocale: 'en',
    locales: [
      { code: 'en', language: 'en-US', name: 'English', file: 'en.json' },
      { code: 'fr', language: 'fr-FR', name: 'Français', file: 'fr.json' }
    ],
    langDir: 'locales',
    detectBrowserLanguage: {
      useCookie: false,
      redirectOn: 'root',
      alwaysRedirect: false,
      fallbackLocale: 'en'
    }
  },
  robots: {
    sitemap: ['/sitemap.xml'],
    disallow: ['/admin/**', '/dashboard/**', '/auth/**', '/enroll']
  },
  sitemap: {
    sitemaps: true,
    autoI18n: true,
    exclude: ['/admin/**', '/dashboard/**', '/auth/**', '/enroll']
  },
  site: {
    url: 'https://product-data-api.com',
    name: 'Product Data API'
  },
  vite: {
    optimizeDeps: {
      include: [
        '@plausible-analytics/tracker',
        'md-editor-v3',
        '@vavt/cm-extension/dist/locale/fr-FR',
        'particles.js',
      ]
    },
    ssr: {
      noExternal: ['vuetify']
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler',
          silenceDeprecations: ['legacy-js-api'],
          style: 'expanded'
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any,
        sass: {
          api: 'modern-compiler',
          silenceDeprecations: ['legacy-js-api'],
          style: 'expanded'
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any
      }
    }
  },
  hooks: {
    'vite:extendConfig': (config) => {
      if (!config.plugins) {
        return
      }

      config.plugins.push(vuetify({ autoImport: true }))
    }
  },
  typescript: {
    tsConfig: {
      exclude: ['./generated/**']
    }
  },
  content: {
    database: {
      type: 'sqlite',
      filename: './.data/content/contents.sqlite'
    },
    experimental: {
      sqliteConnector: 'better-sqlite3'
    }
  },
  compatibilityDate: '2025-01-01',
  devtools: { enabled: true }
})
