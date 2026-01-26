import { defineVitestConfig } from '@nuxt/test-utils/config'
import { configDefaults } from 'vitest/config'

export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    testTimeout: 60000,
    hookTimeout: 60000,
    exclude: [...configDefaults.exclude, 'tests/visual/**'],
    setupFiles: ['./vitest-setup.ts'],
  },
  server: {
    watch: {
      ignored: [
        '**/node_modules/**',
        '**/.git/**',
        '**/.nuxt/**',
        '**/.output/**',
      ],
    },
  },
})
