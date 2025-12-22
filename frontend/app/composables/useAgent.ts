import type {
  AgentTemplateDto,
  AgentRequestDto,
  AgentRequestResponseDto,
  AgentActivityDto,
} from '~~/shared/api-client/services/agents.services'

export const useAgent = () => {
  async function listTemplates(): Promise<AgentTemplateDto[]> {
    return await $fetch<AgentTemplateDto[]>('/api/agents/templates')
  }

  async function submitRequest(
    request: AgentRequestDto
  ): Promise<AgentRequestResponseDto> {
    return await $fetch<AgentRequestResponseDto>('/api/agents', {
      method: 'POST',
      body: request,
    })
  }

  async function listActivity(): Promise<AgentActivityDto[]> {
    return await $fetch<AgentActivityDto[]>('/api/agents/activity')
  }

  async function getMailto(agentId: string): Promise<string> {
    return await $fetch<string>('/api/agents/mailto', {
      params: { agentId },
    })
  }

  return {
    listTemplates,
    submitRequest,
    listActivity,
    getMailto,
  }
}
