import { assertPlaygroundPayloadSize, fetchRouterBinaryAsDataUrl } from '../../utils/playgroundProxy'

export default defineEventHandler(async (event) => {
  const body = await readBody<{ apiKey?: string, payload?: Record<string, unknown> }>(event)
  if (!body.apiKey || !body.payload) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_audio_speech_payload' })
  }

  assertPlaygroundPayloadSize(body.payload)
  return await fetchRouterBinaryAsDataUrl(event, '/v1/audio/speech', body.apiKey, body.payload)
})
