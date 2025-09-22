import { ContentApi, Configuration } from '..'
import type { XwikiContentBlocDto } from '..'
import { resolveDomainResolutionFromRuntime } from '~~/shared/utils/domain-language'
import { createDomainLanguageMiddleware } from './utils'

/**
 * Content service for fetching HTML blocs from the backend
 */
export const useContentService = () => {
  const config = useRuntimeConfig()
  const resolution = resolveDomainResolutionFromRuntime()

  const normalizedApiUrl = config.apiUrl.replace(/\/+$/, '')
  const apiConfig = new Configuration({
    basePath: normalizedApiUrl,
    middleware: [createDomainLanguageMiddleware(normalizedApiUrl, resolution.domainLanguage)],
  })
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
