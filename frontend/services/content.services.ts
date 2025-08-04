import { ContentApi, Configuration } from '~/src/api'
import type { XwikiContentBlocDto } from '~/src/api'

/**
 * Content service for fetching HTML blocs from the backend
 */
export class ContentService {
  private readonly api: ContentApi

  constructor() {
    const config = useRuntimeConfig()
    const apiConfig = new Configuration({ basePath: config.apiUrl })
    this.api = new ContentApi(apiConfig)
  }

  /**
   * Retrieve a content bloc by its identifier
   * @param blocId - XWiki bloc identifier
   */
  async getBloc(blocId: string): Promise<XwikiContentBlocDto> {
    return await this.api.contentBloc({ blocId })
  }
}

// Export singleton instance
export const contentService = new ContentService()
