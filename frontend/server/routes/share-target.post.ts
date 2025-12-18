import {
  createError,
  defineEventHandler,
  readBody,
  readMultipartFormData,
  sendRedirect,
} from 'h3'
import { resolveShareIntent } from '~~/shared/utils/share-intent'

type ShareTargetBody = {
  title?: string
  text?: string
  url?: string
}

const SHARE_TARGET_REDIRECT = '/share-target'

const parseMultipartShareBody = async (
  event: Parameters<ReturnType<typeof defineEventHandler>>[0]
): Promise<ShareTargetBody & { fileText?: string | null }> => {
  const formData = await readMultipartFormData(event)
  const payload: ShareTargetBody & { fileText?: string | null } = {}

  if (!formData) {
    return payload
  }

  for (const part of formData) {
    if (!part) {
      continue
    }

    if (part.name === 'title') {
      payload.title = part.data.toString('utf-8')
    }

    if (part.name === 'text') {
      payload.text = part.data.toString('utf-8')
    }

    if (part.name === 'url') {
      payload.url = part.data.toString('utf-8')
    }

    if (!payload.fileText && part.type?.startsWith('text/')) {
      payload.fileText = part.data.toString('utf-8')
    }
  }

  return payload
}

const readShareTargetPayload = async (
  event: Parameters<ReturnType<typeof defineEventHandler>>[0]
): Promise<ShareTargetBody & { fileText?: string | null }> => {
  const contentType = event.node.req.headers['content-type'] ?? ''

  if (contentType.includes('multipart/form-data')) {
    return parseMultipartShareBody(event)
  }

  const body = await readBody<ShareTargetBody | null>(event)

  return {
    title: body?.title,
    text: body?.text,
    url: body?.url,
    fileText: null,
  }
}

export default defineEventHandler(async event => {
  const payload = await readShareTargetPayload(event)
  const resolution = resolveShareIntent(payload)

  if (!resolution.gtin && !resolution.query) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Share payload was empty or invalid',
    })
  }

  const params = new URLSearchParams()
  if (resolution.gtin) {
    params.set('gtin', resolution.gtin)
  }
  if (resolution.query) {
    params.set('q', resolution.query)
  }
  if (payload.url) {
    params.set('url', payload.url)
  }

  event.node.res.setHeader('Cache-Control', 'no-store, max-age=0')

  return sendRedirect(
    event,
    `${SHARE_TARGET_REDIRECT}${params.size ? `?${params.toString()}` : ''}`,
    303
  )
})
