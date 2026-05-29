import { defineEventHandler } from 'h3'

import { handleLocalSitemapResponse } from '~~/server/utils/local-sitemap-response'

export default defineEventHandler(async event => {
  return handleLocalSitemapResponse(event, { sendBody: false })
})
