import { fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

interface LogsRequestBody {
  apiKey?: string
  limit?: number
}

export default defineEventHandler(async (event) => {
  const body = await readBody<LogsRequestBody>(event)
  if (!body.apiKey) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_logs_payload' })
  }

  return await fetchRouterWithMappedErrors(event, '/v1/logs', body.apiKey, 'GET', undefined, {
    limit: Math.max(1, Math.min(100, Number(body.limit || 20)))
  })
})
