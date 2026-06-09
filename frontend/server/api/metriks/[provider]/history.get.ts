import { readMetrikHistory } from '../../../utils/metriks'

export default defineEventHandler(async event => {
  const provider = getRouterParam(event, 'provider') ?? ''
  const ndjson = await readMetrikHistory(provider)
  setHeader(event, 'Content-Type', 'application/x-ndjson; charset=utf-8')
  return ndjson
})
