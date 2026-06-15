import { Buffer } from 'node:buffer'

interface SaveBody { path: string, content: string, message?: string }

export default defineEventHandler(async (event) => {
  const body = await readBody<SaveBody>(event)
  if (!body.path.startsWith('apps/frontend/content/') || !body.path.endsWith('.md')) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid path' })
  }

  const config = useRuntimeConfig(event)
  const token = config.githubToken || process.env.CONTENT_GITHUB_TOKEN || process.env.GITHUB_TOKEN
  if (!token) {
    throw createError({ statusCode: 500, statusMessage: 'Missing CONTENT_GITHUB_TOKEN' })
  }

  const branch = config.githubDocsBranch || process.env.GITHUB_DOCS_BRANCH || 'main'
  const current = await $fetch<{ sha: string }>(`https://api.github.com/repos/open4good/infera/contents/${encodeURIComponent(body.path)}?ref=${encodeURIComponent(branch)}`, {
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/vnd.github+json' }
  })

  return await $fetch(`https://api.github.com/repos/open4good/infera/contents/${encodeURIComponent(body.path)}`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/vnd.github+json' },
    body: {
      message: body.message || `docs: update ${body.path}`,
      content: Buffer.from(body.content, 'utf8').toString('base64'),
      sha: current.sha,
      branch
    }
  })
})
