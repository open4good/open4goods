import { fetchRouterMultipartWithMappedErrors } from '../../utils/playgroundProxy'

export default defineEventHandler(async (event) => {
  const parts = await readMultipartFormData(event)
  if (!parts) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_audio_transcriptions_payload' })
  }

  let apiKey = ''
  const formData = new FormData()

  for (const part of parts) {
    if (part.name === 'apiKey') {
      apiKey = part.data.toString('utf8')
      continue
    }

    if (!part.name) {
      continue
    }

    if (part.filename) {
      formData.append(part.name, new Blob([new Uint8Array(part.data)], { type: part.type || 'application/octet-stream' }), part.filename)
      continue
    }

    formData.append(part.name, part.data.toString('utf8'))
  }

  if (!apiKey || !formData.get('file') || !formData.get('model')) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_audio_transcriptions_payload' })
  }

  return await fetchRouterMultipartWithMappedErrors(event, '/v1/audio/transcriptions', apiKey, formData)
})
