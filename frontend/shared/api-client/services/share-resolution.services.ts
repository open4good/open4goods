import { ofetch } from 'ofetch'
import type { ShareResolutionResponseDto } from '../models/ShareResolutionResponseDto'
import type { ShareResolutionRequestDto } from '../models/ShareResolutionRequestDto'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

export type ShareResolutionFetcher = <T>(
  url: string,
  options?: Parameters<typeof ofetch<T>>[1]
) => Promise<T>

const shareResolutionPath = '/share/resolutions'

const assertServerRuntime = () => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest

  if (!isServerRuntime) {
    throw new Error(
      'useShareResolutionService() is only available on the server runtime.'
    )
  }
}

const buildHeaders = () => {
  const config = createBackendApiConfig()
  const headers = config.headers ?? {}

  return {
    headers,
    basePath: config.basePath,
  }
}

export const useShareResolutionService = (
  domainLanguage: DomainLanguage,
  fetcher: ShareResolutionFetcher = ofetch
) => {
  assertServerRuntime()
  const { basePath, headers } = buildHeaders()

  const createResolution = async (
    payload: ShareResolutionRequestDto
  ): Promise<ShareResolutionResponseDto> =>
    fetcher<ShareResolutionResponseDto>(`${basePath}${shareResolutionPath}`, {
      method: 'POST',
      body: payload,
      headers,
      query: { domainLanguage },
    })

  const getResolution = async (
    token: string
  ): Promise<ShareResolutionResponseDto> =>
    fetcher<ShareResolutionResponseDto>(
      `${basePath}${shareResolutionPath}/${encodeURIComponent(token)}`,
      {
        method: 'GET',
        headers,
        query: { domainLanguage },
      }
    )

  return {
    createResolution,
    getResolution,
  }
}
