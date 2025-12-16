import { PartnerApi } from '..'
import type { AffiliationPartnerDto, StaticPartnerDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export interface PartnersService {
  fetchAffiliationPartners: () => Promise<AffiliationPartnerDto[]>
  fetchEcosystemPartners: () => Promise<StaticPartnerDto[]>
  fetchMentorPartners: () => Promise<StaticPartnerDto[]>
}

export const usePartnerService = (
  domainLanguage: DomainLanguage
): PartnersService => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: PartnerApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'usePartnerService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new PartnerApi(createBackendApiConfig())
    }

    return api
  }

  const fetchAffiliationPartners = async (): Promise<AffiliationPartnerDto[]> =>
    resolveApi().affiliationPartners({ domainLanguage })

  const fetchEcosystemPartners = async (): Promise<StaticPartnerDto[]> =>
    resolveApi().ecosystemPartners({ domainLanguage })

  const fetchMentorPartners = async (): Promise<StaticPartnerDto[]> =>
    resolveApi().mentorPartners({ domainLanguage })

  return {
    fetchAffiliationPartners,
    fetchEcosystemPartners,
    fetchMentorPartners,
  }
}
