import type { SiteConfigInput } from 'site-config-stack'

declare module 'nuxt/schema' {
  interface NuxtConfig {
    site?: SiteConfigInput
  }
  interface NuxtOptions {
    site?: SiteConfigInput
  }
}

export {}
