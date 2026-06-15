import { assertPlaygroundPayloadSize, fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

export default defineEventHandler(async (event) => {
  const body = await readBody<{ apiKey?: string, payload?: Record<string, unknown> }>(event)
  if (!body.apiKey || !body.payload) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_images_payload' })
  }

  assertPlaygroundPayloadSize(body.payload)
  return await fetchRouterWithMappedErrors(event, '/v1/images/generations', body.apiKey, 'POST', body.payload)
})
