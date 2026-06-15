import { fetchRouterWithMappedErrors } from '../../utils/playgroundProxy'

interface LogDetailRequestBody {
  apiKey?: string
  requestId?: string
}

export default defineEventHandler(async (event) => {
  const body = await readBody<LogDetailRequestBody>(event)
  if (!body.apiKey || !body.requestId) {
    throw createError({ statusCode: 400, statusMessage: 'invalid_log_detail_payload' })
  }

  return await fetchRouterWithMappedErrors(event, `/v1/logs/${encodeURIComponent(body.requestId)}`, body.apiKey, 'GET')
})
