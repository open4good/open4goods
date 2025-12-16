/**
 * We use the Nuxt provided ESLint preset when it is available. When linting is
 * executed in an offline environment without the package cache, resolving
 * `@nuxt/eslint-config` fails. In that case we fall back to a minimal flat
 * configuration that keeps linting functional instead of throwing an error.
 */

const nuxtConfigModule = await import('@nuxt/eslint-config').catch(() => null)

const sharedOverrides = [
  {
    rules: {
      'vue/multi-word-component-names': 'off',
      // Disable ESLint rules that could conflict with Prettier formatting
      'vue/html-indent': 'off',
      'vue/html-self-closing': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/html-closing-bracket-spacing': 'off',
      indent: 'off',
      quotes: 'off',
      semi: 'off',
    },
  },
  {
    ignores: ['shared/api-client/**', 'src/api/**'],
  },
  {
    files: [
      'app/**/*.{ts,vue}',
      'app.config.ts',
      'i18n.config.ts',
      'nuxt.config.ts',
    ],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              regex:
                '^~~/shared/api-client/services/(?!auth\\.services(?:$|/))',
              allowTypeImports: true,
              message:
                'Use the Nuxt server route + composable pattern instead of importing generated backend services directly into client code.',
            },
          ],
        },
      ],
    },
  },
]

const config = await resolveConfig()

export default config

async function resolveConfig() {
  if (nuxtConfigModule?.createConfigForNuxt) {
    return nuxtConfigModule
      .createConfigForNuxt({
        features: {
          typescript: true,
        },
      })
      .append(...sharedOverrides)
  }

  console.warn(
    'Using local fallback ESLint configuration because @nuxt/eslint-config could not be resolved.'
  )
  const fallback = await createFallbackConfig()
  const overrides = fallback.hasVuePlugin
    ? sharedOverrides
    : sharedOverrides.map(entry =>
        entry.rules
          ? {
              ...entry,
              rules: Object.fromEntries(
                Object.entries(entry.rules).filter(
                  ([ruleName]) => !ruleName.startsWith('vue/')
                )
              ),
            }
          : entry
      )

  return [...fallback.configs, ...overrides]
}

async function createFallbackConfig() {
  const [jsModule, globalsModule, vueModule, tsParserModule] =
    await Promise.all([
      import('@eslint/js').catch(() => null),
      import('globals').catch(() => null),
      import('eslint-plugin-vue').catch(() => null),
      import('@typescript-eslint/parser').catch(() => null),
    ])

  const globals = globalsModule?.default || globalsModule || {}
  const resolvedGlobals = {
    browser: globals.browser || {},
    es2021: globals.es2021 || {},
    node: globals.node || {},
  }
  const recommendedJsConfig = jsModule?.configs?.recommended || {}
  const vuePlugin = vueModule?.default || vueModule || null
  const tsParser = tsParserModule?.default || tsParserModule || null

  const baseConfig = {
    name: 'nudger/fallback/javascript',
    ...recommendedJsConfig,
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...resolvedGlobals.browser,
        ...resolvedGlobals.es2021,
        ...resolvedGlobals.node,
      },
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
      },
    },
  }

  if (!recommendedJsConfig.rules) {
    baseConfig.rules = {}
  }

  const configs = [baseConfig]

  if (tsParser) {
    configs.push({
      name: 'nudger/fallback/typescript',
      files: ['**/*.{ts,tsx,vue}'],
      languageOptions: {
        parser: tsParser,
        parserOptions: {
          extraFileExtensions: ['.vue'],
          project: false,
          sourceType: 'module',
        },
      },
    })
  }

  if (vuePlugin) {
    const vueFlatConfig =
      vuePlugin.configs?.['flat/vue3-recommended'] ||
      vuePlugin.configs?.['flat/recommended']

    if (vueFlatConfig) {
      const normalized = Array.isArray(vueFlatConfig)
        ? vueFlatConfig
        : [vueFlatConfig]
      normalized.forEach((entry, index) => {
        configs.push({
          name: entry.name || `nudger/fallback/vue-${index}`,
          ...entry,
        })
      })
    } else {
      configs.push({
        name: 'nudger/fallback/vue-basic',
        files: ['**/*.vue'],
        plugins: {
          vue: vuePlugin,
        },
        rules: {},
      })
    }
  }

  return {
    configs,
    hasVuePlugin: Boolean(vuePlugin),
  }
}
