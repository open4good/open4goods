import { CategoriesApi } from '..'
import type { VerticalConfigDto, VerticalConfigFullDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Categories service for handling category-related API calls
 */
export const useCategoriesService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: CategoriesApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useCategoriesService() is only available on the server runtime.')
    }

    if (!api) {
      api = new CategoriesApi(createBackendApiConfig())
    }

    return api
  }

  /**
   * Fetch categories optionally filtered by enabled status
   * @param onlyEnabled - Filter only enabled categories
   * @returns Promise<VerticalConfigDto[]>
   */
  const getCategories = async (onlyEnabled?: boolean): Promise<VerticalConfigDto[]> => {
    try {
      return await resolveApi().categories1({ domainLanguage, onlyEnabled })
    } catch (error) {
      console.error('Error fetching categories:', error)
      throw error
    }
  }

  /**
   * Fetch a category detail by id
   * @param categoryId - Identifier of the category to fetch
   * @returns Promise<VerticalConfigFullDto>
   */
  const getCategoryById = async (
    categoryId: string,
  ): Promise<VerticalConfigFullDto> => {
    try {
      return await resolveApi().category({ categoryId, domainLanguage })
    } catch (error) {
      console.error('Error fetching category detail:', error)
      throw error
    }
  }

  return { getCategories, getCategoryById }
}
