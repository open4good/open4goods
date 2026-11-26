import { getRequestHeader, setResponseHeader } from 'h3'

import type { CategoryHubResponse } from '~~/shared/utils/category-hub'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import {
  findCategoryHubDoc,
  toCategoryHubDto,
} from '../../utils/category-hub-content'

/**
 * Editorial block of a category hub (intro, FAQ, links), from the `categoryHubs` Nuxt Content
 * collection (GOU-2716). `{ hub: null }` when the vertical has no published file: the category
 * page is then unchanged.
 *
 * Preview: with the `categoryHubsPreview` runtime config (NUXT_CATEGORY_HUBS_PREVIEW=true, beta
 * or dev only), a `published: false` file is also served, flagged `published: false` and never
 * cached.
 */
export default defineEventHandler(
  async (event): Promise<CategoryHubResponse> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

    const verticalId = getRouterParam(event, 'verticalId')
    if (!verticalId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Vertical id is required',
      })
    }

    const rawHost =
      getRequestHeader(event, 'x-forwarded-host') ??
      getRequestHeader(event, 'host')
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const { categoryHubsPreview } = useRuntimeConfig(event)
    const doc = await findCategoryHubDoc(
      event,
      decodeURIComponent(verticalId),
      domainLanguage,
      {
        includeUnpublished:
          categoryHubsPreview === true || categoryHubsPreview === 'true',
      }
    )
    if (!doc) {
      return { hub: null }
    }
    if (!doc.published) {
      setResponseHeader(event, 'Cache-Control', 'private, no-store')
    }
    return { hub: toCategoryHubDto(doc) }
  }
)
