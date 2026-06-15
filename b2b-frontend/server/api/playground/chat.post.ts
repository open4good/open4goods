import { assertPlaygroundPayloadSize, fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

interface ChatRequestBody {
  apiKey?: string
  model?: string
  messages?: unknown[]
  temperature?: number
  maxTokens?: number
}

/**
 * Proxies playground chat requests to the Router OpenAI-compatible endpoint.
 */
export default defineEventHandler(async (event) => {
  const body = await readBody<ChatRequestBody>(event)
  if (!body.apiKey || !body.model || !Array.isArray(body.messages)) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_chat_payload' })
  }

  const payload = {
    model: body.model,
    messages: body.messages,
    temperature: body.temperature,
    max_tokens: body.maxTokens,
    stream: false
  }
  assertPlaygroundPayloadSize(payload)

  return await fetchRouterWithMappedErrors(event, '/v1/chat/completions', body.apiKey, 'POST', payload)
})
