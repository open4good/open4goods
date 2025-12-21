import type {
  AgentTemplateDto,
  AgentRequestDto,
  AgentRequestResponseDto,
  AgentActivityDto,
  DomainLanguage,
} from '@/types/agent'

export const useAgent = () => {
  const { $api } = useNuxtApp() // Assuming an axios/fetch wrapper exists, or use useFetch
  const config = useRuntimeConfig()

  async function listTemplates(
    lang: DomainLanguage = 'fr'
  ): Promise<AgentTemplateDto[]> {
    return await $fetch<AgentTemplateDto[]>('/api/front/agents/templates', {
      baseURL: config.public.apiBase, // Adjust base URL as needed
      params: { domainLanguage: lang },
    })
  }

  async function submitRequest(
    request: AgentRequestDto,
    lang: DomainLanguage = 'fr'
  ): Promise<AgentRequestResponseDto> {
    return await $fetch<AgentRequestResponseDto>('/api/front/agents', {
      baseURL: config.public.apiBase,
      method: 'POST',
      body: request,
      params: { domainLanguage: lang },
    })
  }

  async function listActivity(
    lang: DomainLanguage = 'fr'
  ): Promise<AgentActivityDto[]> {
    return await $fetch<AgentActivityDto[]>('/api/front/agents/activity', {
      baseURL: config.public.apiBase,
      params: { domainLanguage: lang },
    })
  }

  async function getMailto(
    agentId: string,
    lang: DomainLanguage = 'fr'
  ): Promise<string> {
    // Mailto endpoint returns a string (text/plain or wrapped?)
    // Controller returns ResponseEntity<String>.
    return await $fetch<string>('/api/front/agents/mailto', {
      baseURL: config.public.apiBase,
      params: { agentId, domainLanguage: lang },
    })
  }

  return {
    listTemplates,
    submitRequest,
    listActivity,
    getMailto,
  }
}
