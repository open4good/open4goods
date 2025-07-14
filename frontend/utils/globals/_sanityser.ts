import DOMPurify from 'dompurify'

/**
 * Sanitize HTML
 * @param {string} rawHtml
 * @returns {string} sanitizedHtml
 */
export function _sanitizeHtml(rawHtml: string) {
  const sanitized = computed(() => DOMPurify.sanitize(rawHtml))
  return {
    sanitizedHtml: sanitized,
  }
}
