import { defineSiteConfig } from 'nuxt-site-config'

import { HOST_DOMAIN_LANGUAGE_MAP } from './shared/utils/domain-language'

const defaultUrl = 'https://nudger.com'

const alternateUrls = Object.values(HOST_DOMAIN_LANGUAGE_MAP).includes('fr')
  ? { fr: 'https://nudger.fr' }
  : {}

export default defineSiteConfig({
  name: 'Nudger',
  url: defaultUrl,
  urls: {
    en: defaultUrl,
    ...alternateUrls,
  },
})
