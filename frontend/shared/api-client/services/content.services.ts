import { ContentApi, Configuration } from '..'
import type { XwikiContentBlocDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'

/**
 * Content service for fetching HTML blocs from the backend
 */
export const useContentService = (domainLanguage: DomainLanguage) => {
  const config = useRuntimeConfig()
  const apiConfig = new Configuration({ basePath: config.apiUrl })
  const api = new ContentApi(apiConfig)

  /**
   * Retrieve a content bloc by its identifier
   * @param blocId - XWiki bloc identifier
   */
  const getBloc = async (blocId: string): Promise<XwikiContentBlocDto> => {
    return await api.contentBloc({ blocId, domainLanguage })
  }

  return { getBloc }
}
