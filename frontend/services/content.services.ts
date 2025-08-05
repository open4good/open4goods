import { ContentApi, Configuration } from '~/src/api'
import type { XwikiContentBlocDto } from '~/src/api'

/**
 * Content service for fetching HTML blocs from the backend
 */
export const useContentService = () => {
  const config = useRuntimeConfig()
  const apiConfig = new Configuration({ basePath: config.apiUrl })
  const api = new ContentApi(apiConfig)

  /**
   * Retrieve a content bloc by its identifier
   * @param blocId - XWiki bloc identifier
   */
  const getBloc = async (blocId: string): Promise<XwikiContentBlocDto> => {
    return await api.contentBloc({ blocId })
  }

  return { getBloc }
}
