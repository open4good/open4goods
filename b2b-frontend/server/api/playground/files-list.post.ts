import { fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

export default defineEventHandler(async (event) => {
  const body = await readBody<{ apiKey?: string }>(event)
  if (!body.apiKey) {
    throw createError({ statusCode: 400, statusMessage: 'api_key_required' })
  }

  return await fetchRouterWithMappedErrors(event, '/v1/files', body.apiKey, 'GET')
})
