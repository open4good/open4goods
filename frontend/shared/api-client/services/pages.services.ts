import { ContentApi } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export interface CmsFullPage {
  htmlContent: string
  id?: string | null
  fullName?: string | null
  wiki?: string | null
  space?: string | null
  name?: string | null
  title?: string | null
  rawTitle?: string | null
  parent?: string | null
  parentId?: string | null
  version?: string | null
  author?: string | null
  authorName?: string | null
  xwikiRelativeUrl?: string | null
  xwikiAbsoluteUrl?: string | null
  syntax?: string | null
  language?: string | null
  majorVersion?: number | null
  minorVersion?: number | null
  hidden?: boolean
  created?: string | null
  creator?: string | null
  creatorName?: string | null
  modified?: string | null
  modifier?: string | null
  modifierName?: string | null
  originalMetadataAuthor?: string | null
  originalMetadataAuthorName?: string | null
  layout?: string | null
  pageTitle?: string | null
  metaTitle?: string | null
  width?: string | null
  metaDescription?: string | null
  editLink?: string | null
  [key: string]: unknown
}

export const usePagesService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ContentApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'usePagesService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new ContentApi(createBackendApiConfig())
    }

    return api
  }

  const getPage = async (pageId: string): Promise<CmsFullPage> => {
    const page = await resolveApi().page({
      xwikiPageId: pageId,
      domainLanguage,
    })
    return page as CmsFullPage
  }

  return { getPage }
}
