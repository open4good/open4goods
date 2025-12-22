import type {
  AgentTemplateDto,
  AgentRequestDto,
  AgentRequestResponseDto,
  AgentActivityDto,
} from '~~/shared/api-client/services/agents.services'

export const useAgent = () => {
  async function listTemplates(
    domainLanguage?: string
  ): Promise<AgentTemplateDto[]> {
    return await $fetch<AgentTemplateDto[]>('/api/agents/templates', {
      params: { domainLanguage },
    })
  }

  async function submitRequest(
    request: AgentRequestDto
  ): Promise<AgentRequestResponseDto> {
    return await $fetch<AgentRequestResponseDto>('/api/agents', {
      method: 'POST',
      body: request,
    })
  }

  async function listActivity(
    domainLanguage?: string
  ): Promise<AgentActivityDto[]> {
    return await $fetch<AgentActivityDto[]>('/api/agents/activity', {
      params: { domainLanguage },
    })
  }

  async function getMailto(
    agentId: string,
    domainLanguage?: string
  ): Promise<string> {
    return await $fetch<string>('/api/agents/mailto', {
      params: { agentId, domainLanguage },
    })
  }

  return {
    listTemplates,
    submitRequest,
    listActivity,
    getMailto,
  }
}
