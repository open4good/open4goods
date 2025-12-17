import {
  GtinDomainLanguageEnum,
  IsbnDomainLanguageEnum,
  OpenDataApi,
  OverviewDomainLanguageEnum,
  type OpenDataDatasetDto,
  type OpenDataOverviewDto,
} from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

type DomainLanguageEnum =
  (typeof GtinDomainLanguageEnum)[keyof typeof GtinDomainLanguageEnum]

const resolveLanguageEnum = (
  domainLanguage: DomainLanguage
): DomainLanguageEnum =>
  domainLanguage === 'fr'
    ? GtinDomainLanguageEnum.Fr
    : GtinDomainLanguageEnum.En

const resolveOverviewLanguageEnum = (domainLanguage: DomainLanguage) =>
  domainLanguage === 'fr'
    ? OverviewDomainLanguageEnum.Fr
    : OverviewDomainLanguageEnum.En

/**
 * Service responsible for fetching OpenData related metadata from the backend API.
 */
export const useOpenDataService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: OpenDataApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useOpenDataService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new OpenDataApi(createBackendApiConfig())
    }

    return api
  }

  const fetchOverview = async (): Promise<OpenDataOverviewDto> => {
    try {
      return await resolveApi().overview({
        domainLanguage: resolveOverviewLanguageEnum(domainLanguage),
      })
    } catch (error) {
      console.error('Error fetching OpenData overview:', error)
      throw error
    }
  }

  const fetchGtinDataset = async (): Promise<OpenDataDatasetDto> => {
    try {
      return await resolveApi().gtin({
        domainLanguage: resolveLanguageEnum(domainLanguage),
      })
    } catch (error) {
      console.error('Error fetching GTIN dataset metadata:', error)
      throw error
    }
  }

  const fetchIsbnDataset = async (): Promise<OpenDataDatasetDto> => {
    try {
      return await resolveApi().isbn({
        domainLanguage: resolveLanguageEnum(domainLanguage),
      })
    } catch (error) {
      console.error('Error fetching ISBN dataset metadata:', error)
      throw error
    }
  }

  return { fetchOverview, fetchGtinDataset, fetchIsbnDataset }
}
