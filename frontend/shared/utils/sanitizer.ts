import { computed, unref, type MaybeRefOrGetter } from 'vue'
import DOMPurify from 'dompurify'

// NOTE: Unsafe script-stripping regular expression removed. Always use DOMPurify for sanitizing HTML.

/**
 * Sanitize HTML
 * @param {string} rawHtml
 * @returns {string} sanitizedHtml
 */
export function _sanitizeHtml(rawHtml: MaybeRefOrGetter<string | null | undefined>) {
  const sanitized = computed(() => {
    try {
      const input = unref(rawHtml) ?? ''
      const purified = DOMPurify.sanitize(input)
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
