import { ContentApi } from '..'
import type { FullPage } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export type CmsFullPage = FullPage & { editLink?: string | null }

export const usePagesService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ContentApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('usePagesService() is only available on the server runtime.')
    }

    if (!api) {
      api = new ContentApi(createBackendApiConfig())
    }

    return api
  }

  const getPage = async (pageId: string): Promise<CmsFullPage> => {
    const page = await resolveApi().page({ xwikiPageId: pageId, domainLanguage })
    return page as CmsFullPage
  }

  return { getPage }
}
