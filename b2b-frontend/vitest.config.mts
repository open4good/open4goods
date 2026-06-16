import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    include: ['**/*.spec.ts'],
    exclude: ['node_modules/**', '.nuxt/**', '.output/**', 'generated/**'],
  },
})
