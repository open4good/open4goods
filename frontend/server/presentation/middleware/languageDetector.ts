import type { H3Event } from 'h3'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import type { DomainLanguage } from '~~/shared/utils/domain-language'

/**
 * Detect language from request headers
 * Extracts the domain language based on the Host header
 */
export const detectLanguage = (event: H3Event): DomainLanguage => {
  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host

  const { domainLanguage } = resolveDomainLanguage(rawHost)
  return domainLanguage
}
