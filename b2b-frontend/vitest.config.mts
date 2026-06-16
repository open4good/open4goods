import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '~': fileURLToPath(new URL('.', import.meta.url)),
    },
  },
  test: {
    projects: [
      // Pure-logic unit tests — plain node environment, fast
      {
        test: {
          name: 'unit',
          environment: 'node',
          include: [
            'utils/**/*.spec.ts',
            'composables/**/*.spec.ts',
            'domains/**/*.spec.ts',
          ],
          exclude: ['node_modules/**', '.nuxt/**', '.output/**', 'generated/**'],
        },
      },
      // Component / render tests — happy-dom, @vue/test-utils
      // Note: does not use the full Nuxt runtime (vitest-environment-nuxt 2.x is not
      // yet compatible with vitest 3.x). Full SSR/route tests belong in Playwright e2e.
      {
        plugins: [vue()],
        resolve: {
          alias: { '~': fileURLToPath(new URL('.', import.meta.url)) },
        },
        test: {
          name: 'component',
          environment: 'happy-dom',
          setupFiles: ['./tests/setup-vue-globals.ts'],
          include: ['tests/component/**/*.spec.ts'],
          exclude: ['node_modules/**', '.nuxt/**', '.output/**', 'generated/**'],
        },
      },
    ],
  },
})
