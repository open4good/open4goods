import { assertPlaygroundPayloadSize, fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

interface EmbeddingsRequestBody {
  apiKey?: string
  model?: string
  input?: string
}

/**
 * Proxies playground embeddings requests to the Router OpenAI-compatible endpoint.
 */
export default defineEventHandler(async (event) => {
  const body = await readBody<EmbeddingsRequestBody>(event)
  if (!body.apiKey || !body.model || !body.input?.trim()) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_embeddings_payload' })
  }

  const payload = {
    model: body.model,
    input: body.input
  }
  assertPlaygroundPayloadSize(payload)

  return await fetchRouterWithMappedErrors(event, '/v1/embeddings', body.apiKey, 'POST', payload)
})
