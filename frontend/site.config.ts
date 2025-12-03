import { defineSiteConfig } from 'nuxt-site-config'

import {
  DEFAULT_DOMAIN_LANGUAGE,
  HOST_DOMAIN_LANGUAGE_MAP,
} from './shared/utils/domain-language'
import type { DomainLanguage } from './shared/utils/domain-language'

type LocaleUrlMap = Record<DomainLanguage, string>

const protocol = 'https://'

const localeUrls = Object.entries(HOST_DOMAIN_LANGUAGE_MAP).reduce(
  (map, [hostname, domainLanguage]) => {
    if (!map[domainLanguage]) {
      map[domainLanguage] = `${protocol}${hostname}`
    }

    return map
  },
  {} as Partial<LocaleUrlMap>,
)

const defaultUrl =
  localeUrls[DEFAULT_DOMAIN_LANGUAGE] ?? `${protocol}nudger.fr`

export default defineSiteConfig({
  name: 'Nudger',
  url: defaultUrl,
  urls: localeUrls,
})
