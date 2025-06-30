import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // Map `@/` to the `src` directory so imports like `@/components` work in tests
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      // Map `#app` to Nuxt's runtime directory to resolve Nuxt internals during tests
      '#app': fileURLToPath(new URL('./node_modules/nuxt/dist/app', import.meta.url)),
      '#imports': fileURLToPath(new URL('./node_modules/nuxt/dist/app', import.meta.url))
    }
  },
  test: {
    // Use jsdom so Vue components have a browser-like DOM during testing
    environment: 'jsdom'
  }
})
