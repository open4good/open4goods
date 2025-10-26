import type { ModuleOptions } from '@nuxtjs/sitemap'

declare module 'nuxt/schema' {
  interface NuxtConfig {
    sitemap?: ModuleOptions
  }
  interface NuxtOptions {
    sitemap?: ModuleOptions
  }
}

export {}
