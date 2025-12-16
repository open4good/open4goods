import { SearchApi } from '..'
import type { GlobalSearchResponseDto, SearchSuggestResponseDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export const useSearchService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: SearchApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useSearchService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new SearchApi(createBackendApiConfig())
    }

    return api
  }

  const executeGlobalSearch = async (
    query: string
  ): Promise<GlobalSearchResponseDto> => {
    const normalizedQuery = query?.trim()

    if (!normalizedQuery) {
      throw new TypeError('Query is required to execute a global search.')
    }

    try {
      return await resolveApi().globalSearch({
        query: normalizedQuery,
        domainLanguage: domainLanguage,
      })
    } catch (error) {
      console.error(
        'Error executing global search',
        { query: normalizedQuery, domainLanguage },
        error
      )
      throw error
    }
  }

  const fetchSearchSuggestions = async (
    query: string
  ): Promise<SearchSuggestResponseDto> => {
    const normalizedQuery = query?.trim()

    if (!normalizedQuery) {
      throw new TypeError('Query is required to fetch search suggestions.')
    }

    try {
      return await resolveApi().suggest({
        query: normalizedQuery,
        domainLanguage,
      })
    } catch (error) {
      console.error(
        'Error fetching search suggestions',
        {
          query: normalizedQuery,
          domainLanguage,
        },
        error
      )

      throw error
    }
  }

  return {
    executeGlobalSearch,
    fetchSearchSuggestions,
  }
}
