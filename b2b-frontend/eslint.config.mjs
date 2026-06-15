import withNuxt from './.nuxt/eslint.config.mjs'

export default withNuxt([
  {
    ignores: [
      '.nuxt/**',
      '.output/**',
      'dist/**',
      'node_modules/**',
      'generated/**'
    ]
  },
  {
    rules: {
      'vue/valid-v-slot': ['error', { allowModifiers: true }],
      'vue/no-undef-components': ['error', {
        'ignorePatterns': [
          'V[A-Z].*', // Vuetify
          'Nuxt[A-Z].*', // Nuxt
          'Landing[A-Z].*',
          'Docs[A-Z].*',
          'Inf[A-Z].*',
          'Content[A-Z].*',
          'Keys[A-Z].*',
          'Models[A-Z].*',
          'Nodes[A-Z].*',
          'CapacityTester[A-Z].*',
          'RevealBlock',
          'GlowBackdrop',
          'HeroNetworkGraph',
          'ContentRenderer',
          'ClientOnly',
          'Teleport',
          'Suspense',
          'Transition',
          'TransitionGroup',
          'KeepAlive',
          'Slot',
          'I18nT',
          'i18n-t',
          'MDC'
        ]
      }]
    }
  }
])
