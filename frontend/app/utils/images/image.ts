/**
 * Simple image utility functions
 */

// Default fallback image
export const DEFAULT_IMAGE = '/nudger-icon-512x512.png'

/**
 * Check if an image URL is valid
 * @param url - The image URL to validate
 * @returns boolean - True if URL is valid
 */
export const isValidImageUrl = (url: string): boolean => {
  if (!url || typeof url !== 'string') return false

  try {
    const urlObj = new URL(url)
    return urlObj.protocol === 'http:' || urlObj.protocol === 'https:'
  } catch {
    return false
  }
}

/**
 * Get image URL with fallback
 * @param imageUrl - The original image URL
 * @param fallback - Optional fallback image URL
 * @returns string - Valid image URL or fallback
 */
export const getImageUrl = (imageUrl: string, fallback?: string): string => {
  const fallbackImage = fallback || DEFAULT_IMAGE

  if (isValidImageUrl(imageUrl)) {
    return imageUrl
  }

  return fallbackImage
}
