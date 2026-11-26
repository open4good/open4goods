/**
 * Set on the SSR request context when an internal call failed (network error
 * or 5xx): the page may render degraded, so it must not be cached
 * (server/utils/html-cache-control.ts, GOU-2700).
 */
export const HTML_CACHE_DEGRADED_CONTEXT_KEY = 'htmlCacheDegraded'
