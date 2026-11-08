const IPV4_LOOPBACK = /^127(?:\.\d{1,3}){3}$/

/**
 * Tells whether a referrer URL points to a local address (localhost,
 * *.localhost, 127.0.0.0/8, [::1], 0.0.0.0). Empty or invalid → false.
 */
export const isLocalReferrer = (referrer: string | null | undefined) => {
  if (!referrer) {
    return false
  }

  let hostname: string
  try {
    hostname = new URL(referrer).hostname.toLowerCase()
  } catch {
    return false
  }

  return (
    hostname === 'localhost' ||
    hostname.endsWith('.localhost') ||
    IPV4_LOOPBACK.test(hostname) ||
    hostname === '[::1]' ||
    hostname === '0.0.0.0'
  )
}
