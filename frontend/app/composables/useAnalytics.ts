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
    isAnalyticsEnabled: isEnabled,
    extractTokenFromLink,
    isClientContribLink,
  }
}
