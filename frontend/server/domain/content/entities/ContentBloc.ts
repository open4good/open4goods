/**
 * Content Bloc domain entity
 * Represents an HTML content block from XWiki
 */
export interface ContentBloc {
  id: string
  content: string
  language: string
  lastModified?: Date
}

/**
 * Factory function to create a ContentBloc
 */
export const createContentBloc = (data: {
  id: string
  content: string
  language?: string
  lastModified?: string | Date
}): ContentBloc => {
  return {
    id: data.id.trim(),
    content: data.content,
    language: data.language ?? 'en-US',
    lastModified: data.lastModified
      ? typeof data.lastModified === 'string'
        ? new Date(data.lastModified)
        : data.lastModified
      : undefined,
  }
}

/**
 * Business logic: Check if content is empty
 */
export const isEmpty = (bloc: ContentBloc): boolean => {
  return !bloc.content || bloc.content.trim().length === 0
}
