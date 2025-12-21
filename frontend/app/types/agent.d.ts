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

export interface AgentRequestDto {
  type: 'FEATURE' | 'QUESTION'
  promptUser: string
  promptTemplateId: string
  promptVisibility?: 'PUBLIC' | 'PRIVATE'
  userHandle?: string
}

export interface AgentRequestResponseDto {
  issueId: string
  issueNumber: number
  issueUrl: string
  status: string
  previewUrl?: string
  promptVisibility: 'PUBLIC' | 'PRIVATE'
}

export interface AgentIssueDto {
  id: string
  issueNumber: number
  title: string
  body: string
  status: string
  url: string
  createdAt: string
  author: string
  labels: string[]
  promptVisibility: 'PUBLIC' | 'PRIVATE'
}

export interface AgentActivityDto {
  issueId: string
  type: 'FEATURE' | 'QUESTION'
  url: string
  status: string
  promptVisibility: 'PUBLIC' | 'PRIVATE'
  summary?: string
}

export type DomainLanguage = 'fr' | 'en'
