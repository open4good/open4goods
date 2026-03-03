import { useI18n } from 'vue-i18n'
export interface PageSection {
  _id: string
  locale: 'fr' | 'en'
  page: string
  section: string
  order: number
  title?: string
  description?: string
  body?: unknown
}

const normalizePageKey = (slugSegments: string[] | undefined): string => {
  if (!slugSegments || slugSegments.length === 0) {
    return 'home'
  }

  return slugSegments.join('/')
}

const normalizeLocale = (localeCode: string): 'fr' | 'en' =>
  localeCode === 'fr-FR' ? 'fr' : 'en'

export const usePageSections = async (slugSegments?: string[]) => {
  const { locale } = useI18n()

  const localeKey = normalizeLocale(locale.value)
  const pageKey = normalizePageKey(slugSegments)

  const { data, error } = await useAsyncData(
    `sections:${localeKey}:${pageKey}`,
    async () => {
      const results = await queryContent<PageSection>()
        .where({ locale: { $eq: localeKey }, page: { $eq: pageKey } })
        .sort({ order: 1 })
        .find()

      return results
    }
  )

  if (error.value) {
    throw createError({
      statusCode: 500,
      statusMessage: 'Failed to load content sections.',
    })
  }

  return {
    pageKey,
    sections: data,
  }
}
