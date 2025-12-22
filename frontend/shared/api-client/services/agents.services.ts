import { type Configuration } from '../configuration' // Base configuration type
import { createBackendApiConfig } from './createBackendApiConfig'
import type { DomainLanguage } from '~~/app/types/agent'

// We mimic the generated structure so we can swap it later easily.
// DTOs (Manually defined for now, should match backend)

export interface AgentTemplateDto {
  id: string
  name: string
  description: string
  icon: string
  promptTemplate: string
  tags: string[]
  allowedRoles: string[]
  publicPromptHistory: boolean
  mailTemplate?: MailTemplateDto
}

export interface MailTemplateDto {
  to: string
  subject: string
  body: string
}

export enum AgentRequestDtoTypeEnum {
  Feature = 'FEATURE',
  Question = 'QUESTION',
}

export enum AgentRequestDtoPromptVisibilityEnum {
  Public = 'PUBLIC',
  Private = 'PRIVATE',
}

export interface AgentRequestDto {
  type: AgentRequestDtoTypeEnum
  promptUser: string
  promptTemplateId: string
  promptVisibility?: AgentRequestDtoPromptVisibilityEnum
  userHandle?: string
}

export interface AgentRequestResponseDto {
  issueId: string
  issueNumber: number
  issueUrl: string
  status: string
  previewUrl?: string
  promptVisibility: AgentRequestDtoPromptVisibilityEnum
}

export interface AgentActivityDto {
  issueId: string
  type: AgentRequestDtoTypeEnum
  url: string
  status: string
  promptVisibility: AgentRequestDtoPromptVisibilityEnum
  summary?: string
}

export interface AgentIssueDto {
  id: string
  issueNumber: number
  title: string
  body: string
  status: string
  url: string
  author: string
  labels: string[]
  promptVisibility: AgentRequestDtoPromptVisibilityEnum
}

// Service Class Implementation
class AgentService {
  private configuration: Configuration
  private basePath: string

  constructor(configuration: Configuration) {
    this.configuration = configuration
    this.basePath = configuration.basePath || 'https://front-api.nudger.fr'
  }

  private async fetch<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers)
    if (this.configuration.apiKey) {
      // Support for X-Shared-Token via createBackendApiConfig
      const token =
        typeof this.configuration.apiKey === 'function'
          ? await this.configuration.apiKey('X-Shared-Token')
          : this.configuration.apiKey
      if (token) headers.set('X-Shared-Token', token)
    }

    // X-Locale header
    // The generated client usually sets this via a function or we handle it in the wrapper.
    // Here we will pass it explicitly if needed or rely on the caller to ensure domainLanguage is correct.
    // However, existing services pattern suggests we just use the configuration.

    // WARNING: Fetch API requires absolute URL or valid base
    const url = `${this.basePath}${path}`
    const response = await fetch(url, { ...init, headers })

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`)
    }

    // Handle void/text responses
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      return (await response.json()) as T
    } else {
      return (await response.text()) as unknown as T
    }
  }

  async listTemplates(domainLanguage: string): Promise<AgentTemplateDto[]> {
    return this.fetch<AgentTemplateDto[]>(
      `/agents/templates?domainLanguage=${domainLanguage}`
    )
  }

  async submitRequest(
    request: AgentRequestDto,
    domainLanguage: string
  ): Promise<AgentRequestResponseDto> {
    return this.fetch<AgentRequestResponseDto>(
      `/agents?domainLanguage=${domainLanguage}`,
      {
        method: 'POST',
        body: JSON.stringify(request),
        headers: {
          'Content-Type': 'application/json',
        },
      }
    )
  }

  async listActivity(domainLanguage: string): Promise<AgentActivityDto[]> {
    return this.fetch<AgentActivityDto[]>(
      `/agents/activity?domainLanguage=${domainLanguage}`
    )
  }

  async getMailto(agentId: string, domainLanguage: string): Promise<string> {
    // response is text/plain
    return this.fetch<string>(
      `/agents/mailto?agentId=${agentId}&domainLanguage=${domainLanguage}`
    )
  }
}

// Export the factory
export const useAgentService = (domainLanguage: string) => {
  const config = createBackendApiConfig(domainLanguage)
  return new AgentService(config)
}
