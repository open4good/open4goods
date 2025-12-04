import { defineSiteConfig } from 'nuxt-site-config'

const urls = {
  fr: 'https://nudger.fr',
} as const

export default defineSiteConfig({
  name: 'Nudger',
  url: urls.fr,
  urls,
})
