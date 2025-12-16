import { CommercialEventsApi } from '..'
import type { CommercialEvent } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useCommercialEventsService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: CommercialEventsApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useCommercialEventsService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new CommercialEventsApi(createBackendApiConfig())
    }

    return api
  }

  const listCommercialEvents = async (): Promise<CommercialEvent[]> => {
    try {
      return await resolveApi().commercialEvents({ domainLanguage })
    } catch (error) {
      console.error('Error fetching commercial events', error)
      throw error
    }
  }

  return { listCommercialEvents }
}
