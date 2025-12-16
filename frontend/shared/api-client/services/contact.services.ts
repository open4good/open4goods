import { ContactApi, SubmitDomainLanguageEnum } from '..'
import type { ContactRequestDto, ContactResponseDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Service wrapper responsible for submitting contact form messages to the backend API.
 */
export const useContactService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: ContactApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useContactService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new ContactApi(createBackendApiConfig())
    }

    return api
  }

  const submitMessage = async (
    payload: ContactRequestDto
  ): Promise<ContactResponseDto> => {
    const language =
      domainLanguage === 'fr'
        ? SubmitDomainLanguageEnum.Fr
        : SubmitDomainLanguageEnum.En

    try {
      return await resolveApi().submit({
        domainLanguage: language,
        contactRequestDto: payload,
      })
    } catch (error) {
      console.error('Error submitting contact message:', error)
      throw error
    }
  }

  return { submitMessage }
}
