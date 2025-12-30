import type {
  AgentTemplateDto,
  AgentRequestDto,
  AgentRequestResponseDto,
  AgentActivityDto,
  AgentIssueDto,
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
    request: AgentRequestDto,
    domainLanguage?: string
  ): Promise<AgentRequestResponseDto> {
    return await $fetch<AgentRequestResponseDto>('/api/agents', {
      method: 'POST',
      body: request,
      params: { domainLanguage },
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

  async function getIssue(
    issueId: string,
    domainLanguage?: string
  ): Promise<AgentIssueDto> {
    return await $fetch<AgentIssueDto>(`/api/agents/${issueId}`, {
      params: { domainLanguage },
    })
  }

  return {
    listTemplates,
    submitRequest,
    listActivity,
    getMailto,
    getIssue,
  }
}
