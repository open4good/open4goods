import type { Middleware } from '..'
import type { DomainLanguage } from '~~/shared/utils/domain-language'

export const createDomainLanguageMiddleware = (
  apiUrl: string,
  domainLanguage: DomainLanguage
): Middleware => ({
  async pre({ url, init }) {
    if (!url.startsWith(apiUrl)) {
      return
    }

    const targetUrl = new URL(url)
    if (!targetUrl.searchParams.has('domainLanguage')) {
      targetUrl.searchParams.set('domainLanguage', domainLanguage)
      return { url: targetUrl.toString(), init }
    }
  },
})
