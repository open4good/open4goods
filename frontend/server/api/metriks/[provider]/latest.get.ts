import { readMetrikLatest } from '../../../utils/metriks'

export default defineEventHandler(async event => {
  const provider = getRouterParam(event, 'provider') ?? ''
  const latest = await readMetrikLatest(provider)
  if (latest === null) {
    throw createError({ statusCode: 404, statusMessage: 'Provider not found' })
  }
  return latest
})
