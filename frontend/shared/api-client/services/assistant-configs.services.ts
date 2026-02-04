import { AssistantConfigsApi } from '..'
import type { AssistantConfigDto, NudgeToolConfigDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Assistants service for handling assistant configuration API calls
 */
export const useAssistantConfigsService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: AssistantConfigsApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useAssistantConfigsService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new AssistantConfigsApi(createBackendApiConfig())
    }

    return api
  }

  /**
   * Fetch all assistant configurations.
   * @returns Promise<AssistantConfigDto[]>
   */
  const getAssistantConfigs = async (): Promise<AssistantConfigDto[]> => {
    try {
      return await resolveApi().assistantConfigs({ domainLanguage })
    } catch (error) {
      console.error('Error fetching assistant configs:', error)
      throw error
    }
  }

  /**
   * Fetch a single assistant configuration by id.
   * @param assistantId - Identifier of the assistant to fetch
   * @returns Promise<NudgeToolConfigDto>
   */
  const getAssistantConfigById = async (
    assistantId: string
  ): Promise<NudgeToolConfigDto> => {
    try {
      return await resolveApi().assistantConfig({ assistantId, domainLanguage })
    } catch (error) {
      console.error('Error fetching assistant config detail:', error)
      throw error
    }
  }

  return { getAssistantConfigs, getAssistantConfigById }
}
