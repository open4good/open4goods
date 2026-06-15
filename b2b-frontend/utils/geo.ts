/**
 * Converts an ISO 3166-1 alpha-2 country code to a Unicode flag emoji.
 * 
 * @param countryCode - Two-letter country code (e.g., 'FR', 'US')
 * @returns Flag emoji or original string if not a valid 2-letter code
 */
export function getFlagEmoji(countryCode?: string): string {
  if (!countryCode || countryCode.length !== 2) {
    return countryCode || ''
  }

  const codePoints = countryCode
    .toUpperCase()
    .split('')
    .map(char => 127397 + char.charCodeAt(0))

  try {
    return String.fromCodePoint(...codePoints)
  } catch {
    return countryCode
  }
}

/**
 * Returns a formatted location string with flag if available.
 * 
 * @param country - Country code or name
 * @param city - City name
 * @returns Formatted string (e.g., "🇫🇷 Paris" or "Paris")
 */
export function formatLocation(country?: string, city?: string): string {
  const flag = country && country.length === 2 ? getFlagEmoji(country) : ''
  const parts = [flag, city].filter(Boolean)
  
  if (parts.length === 0) {
    return country || ''
  }

  return parts.join(' ')
}

export type GeoPoint = {
  lat: number
  lng: number
}

/**
 * Parses and validates a coordinate pair before it is used by map renderers.
 * The GeoIP provider can occasionally return empty, out-of-range, or null-island
 * values; those must not be allowed to control the map viewport.
 */
export function normalizeGeoPoint(latValue: unknown, lngValue: unknown): GeoPoint | null {
  const lat = Number(latValue)
  const lng = Number(lngValue)

  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null
  }

  if (isValidLatitude(lat) && isValidLongitude(lng) && !isNullIsland(lat, lng)) {
    return { lat, lng }
  }

  if (isValidLatitude(lng) && isValidLongitude(lat) && !isNullIsland(lng, lat)) {
    return { lat: lng, lng: lat }
  }

  return null
}

function isValidLatitude(value: number): boolean {
  return value >= -90 && value <= 90
}

function isValidLongitude(value: number): boolean {
  return value >= -180 && value <= 180
}

function isNullIsland(lat: number, lng: number): boolean {
  return Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001
}
