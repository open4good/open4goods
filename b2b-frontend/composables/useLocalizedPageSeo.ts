interface LocalizedPageSeoOptions {
  titleKey: string
  descriptionKey: string
  noindex?: boolean
}

/**
 * Applies localized SEO metadata for static SSR pages.
 */
export function useLocalizedPageSeo(options: LocalizedPageSeoOptions) {
  const { t } = useI18n()

  useSeoMeta({
    title: () => t(options.titleKey),
    description: () => t(options.descriptionKey),
    ogTitle: () => t(options.titleKey),
    ogDescription: () => t(options.descriptionKey),
    robots: () => options.noindex ? 'noindex, nofollow' : 'index, follow',
    ogType: 'website',
    twitterCard: 'summary_large_image'
  })
}
