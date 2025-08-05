import { ContentApi, Configuration } from '~/src/api'
import type { XwikiContentBlocDto } from '~/src/api'
import { handleErrors } from '~/utils'

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
    try {
      return await api.contentBloc({ blocId })
    } catch (error) {
      handleErrors._handleError(error, `Failed to fetch bloc ${blocId}`)
    }
  }

  return { getBloc }
}
