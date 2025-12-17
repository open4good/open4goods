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
}

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
  }: ResolveAffiliationRedirectOptions): Promise<AffiliationRedirectResponse> => {
    const apiInstance = resolveApi()
    const language = buildLanguageEnum(method)

    try {
      if (method === 'POST') {
        await apiInstance.redirectPostRaw(
          { token, domainLanguage: language, userAgent },
          async requestContext => ({
            ...requestContext.init,
            redirect: 'manual',
          })
        )
      } else {
        await apiInstance.redirectGetRaw(
          { token, domainLanguage: language, userAgent },
          async requestContext => ({
            ...requestContext.init,
            redirect: 'manual',
          })
        )
      }

      throw new Error(
        'Expected a redirect response from the affiliation endpoint.'
      )
    } catch (error) {
      if (error instanceof ResponseError) {
        if (error.response.status === 301) {
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
