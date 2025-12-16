import { ContentApi } from '..'
import type { XwikiContentBlocDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Content service for fetching HTML blocs from the backend
 */
export const useContentService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ContentApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useContentService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new ContentApi(createBackendApiConfig())
    }

    return api
  }

  /**
   * Retrieve a content bloc by its identifier
   * @param blocId - XWiki bloc identifier
   */
  const getBloc = async (blocId: string): Promise<XwikiContentBlocDto> => {
    return await resolveApi().contentBloc({ blocId, domainLanguage })
  }

  return { getBloc }
}
