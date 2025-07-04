import { createConfigForNuxt } from '@nuxt/eslint-config'

export default createConfigForNuxt({
  features: {
    typescript: true,
  },
})
  .append({
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  })
  .append({
    ignores: ['src/api/**'],
  })
