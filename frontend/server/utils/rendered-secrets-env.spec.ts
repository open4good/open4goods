// @vitest-environment node
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'
import { pathToFileURL } from 'node:url'

import { afterEach, describe, expect, it } from 'vitest'

import nuxtConfig from '../../nuxt.config'

/**
 * Env lines that scripts/deploy/render-service-secrets.sh writes into the
 * frontend-ssr env_file (/opt/open4goods/config/<env>/frontend/.env, GOU-1041),
 * read from the script's table, with the consumer each one must reach.
 */
const script = readFileSync(
  new URL('../../../scripts/deploy/render-service-secrets.sh', import.meta.url),
  'utf8'
)
const renderedKeys = [...script.matchAll(/^frontend \S+ env (\S+) /gm)].map(
  match => match[1]
)

/** NUXT_* lines: overrides of the built runtime config, applied by Nitro at startup. */
const RUNTIME_CONFIG_KEYS: Record<string, string> = {
  NUXT_GOOGLE_OAUTH_CLIENT_ID: 'googleOauthClientId',
  NUXT_GOOGLE_OAUTH_CLIENT_SECRET: 'googleOauthClientSecret',
  NUXT_MACHINE_TOKEN: 'machineToken',
}

/**
 * Other lines, read from the process environment: MACHINE_TOKEN by the token
 * check of publish-frontend.sh (open4goods-config), FRONT_SECURITY_JWT_SECRET by
 * requireAdminAccess at request time (covered by auth.spec.ts).
 */
const PROCESS_ENV_KEYS = ['MACHINE_TOKEN', 'FRONT_SECURITY_JWT_SECRET']

/** Nitro's own env override, resolved from the installed nuxt. */
const loadApplyEnv = async () => {
  const fromFrontend = createRequire(import.meta.url)
  const fromNuxt = createRequire(fromFrontend.resolve('nuxt/package.json'))
  const nitropack = dirname(fromNuxt.resolve('nitropack/package.json'))
  const module = await import(
    pathToFileURL(join(nitropack, 'dist/runtime/internal/utils.env.mjs')).href
  )
  return module.applyEnv as (
    config: Record<string, unknown>,
    options: { prefix: string; altPrefix: string }
  ) => Record<string, unknown>
}

describe('frontend env lines rendered from the GitHub Environment', () => {
  const saved = { ...process.env }

  afterEach(() => {
    process.env = { ...saved }
  })

  it('renders exactly the lines whose consumer is known', () => {
    expect([...renderedKeys].sort()).toEqual(
      [...Object.keys(RUNTIME_CONFIG_KEYS), ...PROCESS_ENV_KEYS].sort()
    )
  })

  it('each NUXT_* line overrides its runtime config key at startup', async () => {
    const applyEnv = await loadApplyEnv()
    for (const [envKey, configKey] of Object.entries(RUNTIME_CONFIG_KEYS)) {
      const runtimeConfig = structuredClone(nuxtConfig.runtimeConfig) as Record<
        string,
        unknown
      >
      expect(runtimeConfig, configKey).toHaveProperty(configKey)

      process.env[envKey] = `rendered-${configKey}`
      applyEnv(runtimeConfig, { prefix: 'NITRO_', altPrefix: 'NUXT_' })

      expect(runtimeConfig[configKey], envKey).toBe(`rendered-${configKey}`)
    }
  })
})
