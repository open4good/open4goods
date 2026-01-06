import type { VerticalCategoryDto } from '~/shared/api-client'

export type NudgeToolCategory = VerticalCategoryDto & {
  externalLink?: string
  tooltip?: string
}
