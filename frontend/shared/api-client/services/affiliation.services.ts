import {
  AffiliationApi,
  RedirectGetDomainLanguageEnum,
  RedirectPostDomainLanguageEnum,
  ResponseError,
} from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export type AffiliationRedirectHttpMethod = 'GET' | 'POST'

export interface ResolveAffiliationRedirectOptions {
  token: string
  userAgent?: string
  method: AffiliationRedirectHttpMethod
  /** Same-site path the click came from, sent as `X-Affiliation-Origin`. */
  origin?: string
}

/**
 * Redirect statuses accepted from the backend: 302 (non-cacheable, current contract) and 301 (previous contract,
 * kept so a frontend deployed before front-api keeps working).
 */
const BACKEND_REDIRECT_STATUSES = new Set([301, 302])

export interface AffiliationRedirectResponse {
  statusCode: number
  location: string
}

/**
 * Service responsible for resolving affiliation redirects through the backend API.
 */
export const useAffiliationService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: AffiliationApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useAffiliationService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new AffiliationApi(createBackendApiConfig())
    }

    return api
  }

  const buildLanguageEnum = (method: AffiliationRedirectHttpMethod) => {
    if (method === 'POST') {
      return domainLanguage === 'fr'
        ? RedirectPostDomainLanguageEnum.Fr
        : RedirectPostDomainLanguageEnum.En
    }

    return domainLanguage === 'fr'
      ? RedirectGetDomainLanguageEnum.Fr
      : RedirectGetDomainLanguageEnum.En
  }

  const resolveRedirect = async ({
    token,
    userAgent,
    method,
    origin,
  }: ResolveAffiliationRedirectOptions): Promise<AffiliationRedirectResponse> => {
    const apiInstance = resolveApi()
    const language = buildLanguageEnum(method)

    try {
      if (method === 'POST') {
        await apiInstance.redirectPostRaw(
          { token, domainLanguage: language, userAgent },
          async requestContext => ({
            ...requestContext.init,
            headers: {
              ...(requestContext.init.headers as Record<string, string>),
              ...(origin ? { 'X-Affiliation-Origin': origin } : {}),
            },
            redirect: 'manual',
          })
        )
      } else {
        await apiInstance.redirectGetRaw(
          { token, domainLanguage: language, userAgent },
          async requestContext => ({
            ...requestContext.init,
            headers: {
              ...(requestContext.init.headers as Record<string, string>),
              ...(origin ? { 'X-Affiliation-Origin': origin } : {}),
            },
            redirect: 'manual',
          })
        )
      }

      throw new Error(
        'Expected a redirect response from the affiliation endpoint.'
      )
    } catch (error) {
      if (error instanceof ResponseError) {
        if (BACKEND_REDIRECT_STATUSES.has(error.response.status)) {
          const location = error.response.headers.get('Location') ?? undefined

          if (!location) {
            throw new Error('Redirect response is missing the Location header.')
          }

          return {
            statusCode: error.response.status,
            location,
          }
        }
      }

      throw error
    }
  }

  return {
    resolveRedirect,
  }
}
