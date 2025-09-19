import { createConfigForNuxt } from '@nuxt/eslint-config'

export default createConfigForNuxt({
  features: {
    typescript: true,
  },
})
  .append({
    rules: {
      'vue/multi-word-component-names': 'off',
      // Désactiver les règles ESLint qui peuvent entrer en conflit avec Prettier
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
    ignores: ['app/src/api/**'],
  })
