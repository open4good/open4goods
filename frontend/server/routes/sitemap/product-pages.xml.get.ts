import { defineEventHandler, sendRedirect } from 'h3'

export default defineEventHandler(event => {
  return sendRedirect(event, '/sitemap/fr/product-pages.xml', 301)
})
