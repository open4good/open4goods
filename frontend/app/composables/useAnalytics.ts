import { useNuxtApp, useRuntimeConfig } from '#imports'
import { isDoNotTrackEnabled } from '~/utils/do-not-track'

type PlausibleTracker = {
  trackEvent: (
    eventName: string,
    options?: {
      props?: Record<string, unknown>
      url?: string
      referrer?: string
    }
  ) => void
}

type SearchSource = 'form' | 'suggestion'

type ProductRedirectContext = {
  token?: string | null
  placement?: string
  source?: string | null
  url?: string | null
}

type OpenDataDownloadContext = {
  datasetId: string
  method: string
  href?: string
}

type AffiliateClickContext = {
  token?: string | null
  url?: string | null
  partner?: string | null
  placement?: string | null
  productId?: string | number | null
}

type TabClickContext = {
  tab: string
  context: string
  label?: string | null
  productId?: string | number | null
}

type SearchFocusContext = {
  location: string
  queryLength?: number
}

type FileDownloadContext = {
  fileType: string
  url?: string | null
  label?: string | null
  context?: string | null
}

type SectionViewContext = {
  sectionId: string
  page?: string | null
  label?: string | null
}

type FilterChangeContext = {
  categoryId?: string | number | null
  categorySlug?: string | null
  action: string
  source?: string | null
  filtersCount?: number
  filterFields?: string[]
  subsetIds?: string[]
}

type SearchContext = {
  query: string
  source: SearchSource
  results?: number | null
}

const isClientContribLink = (link?: string | null): link is string =>
  Boolean(link && link.startsWith('/contrib/'))

const extractTokenFromLink = (link?: string | null) =>
  isClientContribLink(link)
    ? (link.split('/').filter(Boolean).pop() ?? null)
    : null

export const useAnalytics = () => {
  const nuxtApp = useNuxtApp()
  const runtimeConfig = useRuntimeConfig()

  const isEnabled = () => {
    if (!import.meta.client) {
      return false
    }

    if (isDoNotTrackEnabled()) {
      return false
    }

    return Boolean(
      runtimeConfig.public.plausible?.enabled && nuxtApp.$plausible
    )
  }

  const trackEvent = (
    eventName: string,
    options?: { props?: Record<string, unknown>; url?: string }
  ) => {
    if (!isEnabled()) {
      return
    }

    const plausible = nuxtApp.$plausible as PlausibleTracker | undefined
    plausible?.trackEvent(eventName, options)
  }

  const trackAffiliateClick = ({
    token,
    url,
    partner,
    placement,
    productId,
  }: AffiliateClickContext) => {
    const resolvedToken = token ?? extractTokenFromLink(url)

    if (!resolvedToken) {
      return
    }

    trackEvent('affiliate-click', {
      props: {
        token: resolvedToken,
        url,
        partner,
        placement,
        productId,
      },
    })
  }

  const trackTabClick = ({
    tab,
    context,
    label,
    productId,
  }: TabClickContext) => {
    if (!tab) {
      return
    }

    trackEvent('tab-click', {
      props: {
        tab,
        context,
        label,
        productId,
      },
    })
  }

  const trackSearchFocus = ({ location, queryLength }: SearchFocusContext) => {
    trackEvent('search-focus', {
      props: {
        location,
        queryLength,
      },
    })
  }

  const trackFileDownload = ({
    fileType,
    url,
    label,
    context,
  }: FileDownloadContext) => {
    trackEvent('file-download', {
      props: {
        fileType,
        url,
        label,
        context,
      },
    })
  }

  const trackSectionView = ({ sectionId, page, label }: SectionViewContext) => {
    if (!sectionId) {
      return
    }

    trackEvent('section-view', {
      props: {
        sectionId,
        page,
        label,
      },
    })
  }

  const trackFilterChange = ({
    categoryId,
    categorySlug,
    action,
    source,
    filtersCount,
    filterFields,
    subsetIds,
  }: FilterChangeContext) => {
    trackEvent('category-filter-change', {
      props: {
        categoryId,
        categorySlug,
        action,
        source,
        filtersCount,
        filterFields,
        subsetIds,
      },
    })
  }

  const trackOpenDataDownload = ({
    datasetId,
    method,
    href,
  }: OpenDataDownloadContext) => {
    trackEvent('open-data-download', {
      props: {
        dataset: datasetId,
        method,
        href,
      },
    })
  }

  const trackProductRedirect = ({
    token,
    placement,
    source,
    url,
  }: ProductRedirectContext) => {
    const resolvedToken = token ?? extractTokenFromLink(url)

    if (!resolvedToken) {
      return
    }

    trackEvent('product-redirect', {
      props: {
        token: resolvedToken,
        placement,
        source,
        url,
      },
    })
  }

  const trackSearch = ({ query, source, results }: SearchContext) => {
    const trimmedQuery = query.trim()

    if (!trimmedQuery) {
      return
    }

    trackEvent('search', {
      props: {
        query: trimmedQuery,
        source,
        results,
      },
    })
  }

  return {
    trackEvent,
    trackOpenDataDownload,
    trackProductRedirect,
    trackSearch,
    trackAffiliateClick,
    trackTabClick,
    trackSearchFocus,
    trackFileDownload,
    trackSectionView,
    trackFilterChange,
    isAnalyticsEnabled: isEnabled,
    extractTokenFromLink,
    isClientContribLink,
  }
}
