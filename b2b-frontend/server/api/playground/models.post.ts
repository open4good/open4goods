import { fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

/**
 * Proxies model listing requests for a provided playground API key.
 */
export default defineEventHandler(async (event) => {
  const { apiKey } = await readBody<{ apiKey?: string }>(event)
  if (!apiKey || !apiKey.trim()) {
    throw createError({ statusCode: 400, statusMessage: 'api_key_required' })
  }

  return await fetchRouterWithMappedErrors(event, '/v1/models', apiKey, 'GET')
})
