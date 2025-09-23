import { createConfigForNuxt } from '@nuxt/eslint-config'

export default createConfigForNuxt({
  features: {
    typescript: true,
  },
})
  .append({
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
  })
  .append({
    ignores: ['shared/api-client/**', 'src/api/**'],
  })
