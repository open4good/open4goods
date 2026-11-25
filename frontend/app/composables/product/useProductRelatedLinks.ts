import type { Ref } from 'vue'
import type {
  FilterRequestDto,
  ProductDto,
  ProductSearchResponseDto,
  VerticalConfigFullDto,
  VerticalSubCategoryDto,
} from '~~/shared/api-client'
import { CATEGORY_INITIAL_SORT } from '~/utils/_category-initial-sort'
import { mergeFilterRequests } from '~/utils/_merge-filter-requests'
import { buildFilterRequestFromCriteria } from '~/utils/_subset-to-filters'
import { pickRelatedProducts } from '~/utils/_related-links'

const SEARCH_POOL_SIZE = 16

export interface ProductRelatedLinks {
  subCategory: { title: string; href: string } | null
  products: ProductDto[]
}

const joinPath = (base: string, slug: string) =>
  `${base.replace(/\/+$/, '')}/${slug.replace(/^\/+/, '')}`

/**
 * Resolves, during SSR, the main sub-category of a product (the first one whose filters
 * match it) and the products sharing it, so the product page links them with plain `<a>`.
 * Failures degrade to the vertical's best products: this block must never break the page.
 */
export const useProductRelatedLinks = async (
  product: Ref<ProductDto | null | undefined>,
  vertical: Ref<VerticalConfigFullDto | null>
) => {
  const { $fetch: csrfFetch } = useNuxtApp()

  const search = async (
    verticalId: string,
    filters: FilterRequestDto | undefined,
    pageSize: number
  ) => {
    const response = await csrfFetch<ProductSearchResponseDto>(
      '/api/products/search',
      {
        method: 'POST',
        query: { include: 'base,identity,names,scores' },
        body: {
          verticalId,
          pageNumber: 0,
          pageSize,
          sort: CATEGORY_INITIAL_SORT,
          filters,
        },
      }
    )
    return response.products?.data ?? []
  }

  const { data } = await useAsyncData<ProductRelatedLinks>(
    `product-related-${product.value?.gtin ?? 'none'}`,
    async () => {
      const verticalId = vertical.value?.id
      const gtin = product.value?.gtin
      const empty: ProductRelatedLinks = { subCategory: null, products: [] }
      if (!verticalId || gtin == null) {
        return empty
      }

      try {
        const base = vertical.value?.verticalHomeUrl?.trim() ?? ''
        const candidates = (vertical.value?.subCategories ?? []).filter(
          (sub): sub is VerticalSubCategoryDto & { slug: string } =>
            Boolean(sub.slug?.trim() && sub.activatedFilters?.length)
        )
        const gtinFilter: FilterRequestDto = {
          filters: [{ field: 'gtin', operator: 'term', terms: [String(gtin)] }],
        }
        const subFilters = (sub: VerticalSubCategoryDto) =>
          buildFilterRequestFromCriteria(sub.activatedFilters ?? [])

        const memberships = await Promise.all(
          candidates.map(async sub => {
            try {
              const hits = await search(
                verticalId,
                mergeFilterRequests(subFilters(sub), gtinFilter),
                1
              )
              return hits.length > 0
            } catch {
              return false
            }
          })
        )
        const main = candidates.find((_, index) => memberships[index])

        const neighbours = await search(
          verticalId,
          main ? subFilters(main) : undefined,
          SEARCH_POOL_SIZE
        )

        return {
          subCategory:
            main && base
              ? {
                  title: main.h1Title?.trim() || main.slug,
                  href: joinPath(base, main.slug),
                }
              : null,
          products: pickRelatedProducts(neighbours, gtin),
        }
      } catch (error) {
        console.error('[ProductPage] Failed to load related products.', error)
        return empty
      }
    },
    { server: true, immediate: true }
  )

  return computed<ProductRelatedLinks>(
    () => data.value ?? { subCategory: null, products: [] }
  )
}
