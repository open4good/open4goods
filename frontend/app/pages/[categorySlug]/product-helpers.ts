import { createError } from 'nuxt/app'

const CATEGORY_PATTERN = /^[a-z]+(?:-[a-z]+)*$/
const PRODUCT_SLUG_PATTERN = /^(\d{6,})(-.+)?$/

export const validateCategorySlug = (slug: string): string => {
  if (!CATEGORY_PATTERN.test(slug)) {
    throw createError({ statusCode: 404, statusMessage: 'Page not found' })
  }

  return slug
}

export const extractGtinFromProductSlug = (slug: string): string => {
  const match = PRODUCT_SLUG_PATTERN.exec(slug)

  if (!match) {
    throw createError({ statusCode: 404, statusMessage: 'Page not found' })
  }

  const [, gtin] = match

  if (!gtin) {
    throw createError({ statusCode: 404, statusMessage: 'Page not found' })
  }

  return gtin
}

export const normalizeFullSlug = (fullSlug: string | null | undefined): string | null => {
  if (!fullSlug) {
    return null
  }

  return fullSlug.replace(/^\/+/, '')
}
