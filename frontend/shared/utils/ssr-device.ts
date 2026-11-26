const MOBILE_USER_AGENT_PATTERN =
  /(Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Kindle|Silk|Opera Mini)/i

export interface SsrDeviceHeaders {
  /** Cloudflare device class (`mobile`, `tablet`, `desktop`) when the edge caches by device type. */
  cfDeviceType?: string | null
  userAgent?: string | null
}

/**
 * Whether the server renders the desktop layout. Cloudflare's `CF-Device-Type`
 * wins over the User-Agent: it is the device class of the edge cache key, so a
 * cached page always matches the key it is stored under (GOU-2700).
 */
export const isDesktopRequest = ({
  cfDeviceType,
  userAgent,
}: SsrDeviceHeaders) => {
  const deviceType = cfDeviceType?.trim().toLowerCase()

  if (deviceType === 'desktop') {
    return true
  }

  if (deviceType === 'mobile' || deviceType === 'tablet') {
    return false
  }

  if (!userAgent) {
    return true
  }

  return !MOBILE_USER_AGENT_PATTERN.test(userAgent)
}
