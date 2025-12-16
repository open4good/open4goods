import { AdministrationApi } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useAdministrationService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: AdministrationApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useAdministrationService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new AdministrationApi(createBackendApiConfig())
    }

    return api
  }

  const resetCache = async (): Promise<void> => {
    try {
      await resolveApi().resetCache()
    } catch (error) {
      console.error('Error resetting backend caches:', error)
      throw error
    }
  }

  return { resetCache }
}
