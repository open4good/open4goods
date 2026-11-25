import { sendRedirect } from 'h3'

// Pages whose trailing-slash variant must 301 to the canonical URL instead of
// answering 200 (duplicate content, SEO audit 2026-11-25).
const TRAILING_SLASH_PATHS = [
  /^(?:\/[a-z]{2}(?:-[A-Z]{2})?)?\/impact-score\/+$/u,
  /^(?:\/[a-z]{2}(?:-[A-Z]{2})?)?\/[^/]+\/ecoscore\/+$/u,
]

export default defineEventHandler(event => {
  if (event.method !== 'GET' && event.method !== 'HEAD') {
    return
  }

  const rawPath = event.path ?? event.node.req.url ?? '/'
  const queryIndex = rawPath.indexOf('?')
  const pathname = queryIndex === -1 ? rawPath : rawPath.slice(0, queryIndex)
  const query = queryIndex === -1 ? '' : rawPath.slice(queryIndex)

  if (!TRAILING_SLASH_PATHS.some(pattern => pattern.test(pathname))) {
    return
  }

  return sendRedirect(event, `${pathname.replace(/\/+$/u, '')}${query}`, 301)
})
