import { SearchApi } from '..'
import type { GlobalSearchResponseDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useSearchService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: SearchApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useSearchService() is only available on the server runtime.')
    }

    if (!api) {
      api = new SearchApi(createBackendApiConfig())
    }

    return api
  }

  const globalSearch = async (query: string): Promise<GlobalSearchResponseDto> => {
    const normalizedQuery = query?.trim()

    if (!normalizedQuery) {
      throw new TypeError('Search query must be a non-empty string.')
    }

    try {
      return await resolveApi().globalSearch({
        query: normalizedQuery,
        domainLanguage,
      })
    } catch (error) {
      console.error('Error executing global search with query:', normalizedQuery, error)
      throw error
    }
  }

  return { globalSearch }
}
