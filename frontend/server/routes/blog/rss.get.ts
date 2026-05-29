import { createError, defineEventHandler } from 'h3'

export default defineEventHandler(() => {
  throw createError({
    statusCode: 410,
    statusMessage: 'RSS feed retired',
  })
})
