import { GetQuotaStatusDomainLanguageEnum, IpQuotaCategory, QuotaApi } from '..'
import type { IpQuotaStatusDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

const DOMAIN_LANGUAGE_TO_QUOTA_MAP: Record<
  DomainLanguage,
  GetQuotaStatusDomainLanguageEnum
> = {
  fr: GetQuotaStatusDomainLanguageEnum.Fr,
  en: GetQuotaStatusDomainLanguageEnum.En,
}

/**
 * Service wrapper around the Quota API. Only usable from the server runtime.
 */
export const useQuotaService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: QuotaApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useQuotaService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new QuotaApi(createBackendApiConfig())
    }

    return api
  }

  const getQuotaStatus = async (
    category: IpQuotaCategory
  ): Promise<IpQuotaStatusDto> => {
    return resolveApi().getQuotaStatus({
      category,
      domainLanguage: DOMAIN_LANGUAGE_TO_QUOTA_MAP[domainLanguage],
    })
  }

  return {
    getQuotaStatus,
  }
}
