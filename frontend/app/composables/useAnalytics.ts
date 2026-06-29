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
  affiliatePlatform?: string | null
  merchantId?: string | number | null
  merchantName?: string | null
  merchantSlug?: string | null
  partner?: string | null
  placement?: string | null
  productId?: string | number | null
  gtin?: string | number | null
  vertical?: string | null
  categorySlug?: string | null
  offerRank?: number | null
  price?: number | null
  currency?: string | null
  condition?: string | null
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

type SortUsageContext = {
  categoryId?: string | number | null
  categorySlug?: string | null
  action: 'exposed' | 'selected' | 'order-updated'
  selectedField?: string | null
  selectedGroup?: 'primary' | 'advanced' | 'unknown'
  sortOrder?: 'asc' | 'desc'
  defaultField?: string | null
  primaryOptions?: string[]
  advancedOptions?: string[]
  totalOptions?: number
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

const compactProps = (props: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(props).filter(([, value]) => {
      if (value === null || value === undefined) {
        return false
      }

      return typeof value !== 'string' || value.trim().length > 0
    })
  )

export const normalizeAnalyticsSlug = (value?: string | number | null) => {
  const normalized = String(value ?? '')
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

  return normalized || null
}

export const resolveAnalyticsHost = (url?: string | null) => {
  if (!url || url.startsWith('/')) {
    return null
  }

  try {
    return new URL(url).hostname.replace(/^www\./, '') || null
  } catch {
    return null
  }
}

export const resolvePriceBucket = (price?: number | null) => {
  if (typeof price !== 'number' || Number.isNaN(price) || price < 0) {
    return null
  }

  if (price < 25) return '0-25'
  if (price < 50) return '25-50'
  if (price < 100) return '50-100'
  if (price < 250) return '100-250'
  if (price < 500) return '250-500'
  if (price < 1000) return '500-1000'
  return '1000-plus'
}

export const buildAffiliateClickProps = ({
  token,
  url,
  affiliatePlatform,
  merchantId,
  merchantName,
  merchantSlug,
  partner,
  placement,
  productId,
  gtin,
  vertical,
  categorySlug,
  offerRank,
  price,
  currency,
  condition,
}: AffiliateClickContext) => {
  const resolvedMerchantName = merchantName ?? partner ?? null

  return compactProps({
    token,
    affiliatePlatform: affiliatePlatform ?? 'unknown',
    merchantId,
    merchantName: resolvedMerchantName,
    merchantSlug:
      merchantSlug ?? normalizeAnalyticsSlug(resolvedMerchantName ?? merchantId),
    placement,
    productId,
    gtin,
    vertical,
    categorySlug,
    offerRank,
    priceBucket: resolvePriceBucket(price),
    currency,
    condition,
    destinationHost: resolveAnalyticsHost(url),
  })
}

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
    affiliatePlatform,
    merchantId,
    merchantName,
    merchantSlug,
    partner,
    placement,
    productId,
    gtin,
    vertical,
    categorySlug,
    offerRank,
    price,
    currency,
    condition,
  }: AffiliateClickContext) => {
    const resolvedToken = token ?? extractTokenFromLink(url)

    if (!resolvedToken) {
      return
    }

    trackEvent('affiliate-click', {
      props: buildAffiliateClickProps({
        token: resolvedToken,
        url,
        affiliatePlatform,
        merchantId,
        merchantName,
        merchantSlug,
        partner,
        placement,
        productId,
        gtin,
        vertical,
        categorySlug,
        offerRank,
        price,
        currency,
        condition,
      }),
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

  const trackSortUsage = ({
    categoryId,
    categorySlug,
    action,
    selectedField,
    selectedGroup,
    sortOrder,
    defaultField,
    primaryOptions,
    advancedOptions,
    totalOptions,
  }: SortUsageContext) => {
    trackEvent('category-sort-usage', {
      props: {
        categoryId,
        categorySlug,
        action,
        selectedField,
        selectedGroup,
        sortOrder,
        defaultField,
        primaryOptions,
        advancedOptions,
        totalOptions,
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
    trackSortUsage,
    isAnalyticsEnabled: isEnabled,
    extractTokenFromLink,
    isClientContribLink,
  }
}
