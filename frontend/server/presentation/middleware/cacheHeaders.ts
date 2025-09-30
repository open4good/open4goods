import type { H3Event } from 'h3'

/**
 * Cache strategy configuration
 */
export interface CacheConfig {
  maxAge?: number // Cache duration in seconds
  sMaxAge?: number // CDN cache duration in seconds
  public?: boolean // Allow public caching
  private?: boolean // Only browser caching
  noCache?: boolean // Disable caching
  mustRevalidate?: boolean // Must revalidate with server
}

/**
 * Apply cache headers to response
 */
export const applyCacheHeaders = (event: H3Event, config: CacheConfig) => {
  const directives: string[] = []

  if (config.noCache) {
    directives.push('no-cache', 'no-store', 'must-revalidate')
  } else {
    if (config.public) {
      directives.push('public')
    }
    if (config.private) {
      directives.push('private')
    }
    if (config.maxAge !== undefined) {
      directives.push(`max-age=${config.maxAge}`)
    }
    if (config.sMaxAge !== undefined) {
      directives.push(`s-maxage=${config.sMaxAge}`)
    }
    if (config.mustRevalidate) {
      directives.push('must-revalidate')
    }
  }

  if (directives.length > 0) {
    setResponseHeader(event, 'Cache-Control', directives.join(', '))
  }
}

/**
 * Predefined cache strategies
 */
export const CacheStrategies = {
  /** Cache for 1 hour (3600s) */
  ONE_HOUR: {
    public: true,
    maxAge: 3600,
    sMaxAge: 3600,
  } as CacheConfig,

  /** Cache for 5 minutes (300s) */
  FIVE_MINUTES: {
    public: true,
    maxAge: 300,
    sMaxAge: 300,
  } as CacheConfig,

  /** No caching */
  NO_CACHE: {
    noCache: true,
  } as CacheConfig,

  /** Private browser cache only */
  PRIVATE: {
    private: true,
    maxAge: 300,
  } as CacheConfig,
}
