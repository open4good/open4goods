import { computed } from 'vue'
import DOMPurify from 'dompurify'

// NOTE: Unsafe script-stripping regular expression removed. Always use DOMPurify for sanitizing HTML.

/**
 * Sanitize HTML
 * @param {string} rawHtml
 * @returns {string} sanitizedHtml
 */
export function _sanitizeHtml(rawHtml: string) {
  const sanitized = computed(() => {
    try {
      const purified = DOMPurify.sanitize(rawHtml)
      // If DOMPurify fails or produces falsy, fallback to empty string
      return purified || ''
    } catch {
      // If DOMPurify throws, fallback to empty string
      return ''
    }
  })
  return {
    sanitizedHtml: sanitized,
  }
}
