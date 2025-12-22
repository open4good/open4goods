import { defineVitestConfig } from '@nuxt/test-utils/config'
import { configDefaults } from 'vitest/config'

export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    testTimeout: 15000,
    exclude: [...configDefaults.exclude, 'tests/visual/**'],
  },
})
