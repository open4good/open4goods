/**
 * Represents an external link (social media, survey, etc.)
 */
export interface ExternalLink {
  url: string
  label: string
  icon?: string
  type?: 'linkedin' | 'twitter' | 'github' | 'survey' | 'generic'
}

/**
 * Configuration for the next release announcement section
 */
export interface NextReleaseConfig {
  enabled: boolean
  title: string
  description: string
  xwikiContentId?: string
  releaseVersion?: string
  targetDate?: string
  externalLinks?: ExternalLink[]
}
