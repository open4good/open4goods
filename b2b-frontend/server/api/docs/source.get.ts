import { Buffer } from 'node:buffer'

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const path = String(query.path || '')
  if (!path.startsWith('apps/frontend/content/') || !path.endsWith('.md')) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid path' })
  }

  const config = useRuntimeConfig(event)
  const token = config.githubToken || process.env.CONTENT_GITHUB_TOKEN || process.env.GITHUB_TOKEN
  if (!token) {
    throw createError({ statusCode: 500, statusMessage: 'Missing CONTENT_GITHUB_TOKEN' })
  }

  const response = await $fetch<{ content: string }>(`https://api.github.com/repos/open4good/infera/contents/${encodeURIComponent(path)}`, {
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/vnd.github+json' }
  })

  return { content: Buffer.from(response.content, 'base64').toString('utf8') }
})
