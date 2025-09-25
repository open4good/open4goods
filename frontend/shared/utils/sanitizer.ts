import { computed } from 'vue'
import DOMPurify from 'dompurify'

const stripScripts = (html: string) => html.replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')

/**
 * Sanitize HTML
 * @param {string} rawHtml
 * @returns {string} sanitizedHtml
 */
export function _sanitizeHtml(rawHtml: string) {
  const sanitized = computed(() => {
    try {
      const purified = DOMPurify.sanitize(rawHtml)
      return purified || stripScripts(rawHtml)
    } catch {
      return stripScripts(rawHtml)
    }
  })
  return {
    sanitizedHtml: sanitized,
  }
}
