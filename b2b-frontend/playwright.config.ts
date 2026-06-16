import { defineConfig, devices } from '@playwright/test'

const PORT = 3000
const BASE_URL = `http://localhost:${PORT}`

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? 'github' : 'list',

  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: {
    // Preview a production build — fastest for e2e; ensures SSR matches what's served
    command: 'pnpm build && pnpm preview',
    port: PORT,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      // Point backend at a stub/mock — real backend not required for CI e2e
      NUXT_PUBLIC_BACKEND_BASE_URL: `http://localhost:${PORT}/__mock_backend__`,
    },
  },
})
