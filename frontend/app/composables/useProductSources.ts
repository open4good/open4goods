import type { InjectionKey, Ref } from 'vue'
import { computed, inject, provide } from 'vue'
import type { ProductSourceDto } from '~~/shared/api-client'

/**
 * Sources of the product page (GOU-2302), indexed by normalised name.
 *
 * The product DTO lists each source once (`datasources.sources`); the attribute
 * sourcing only names it (`ProductSourcedAttributeDto.datasourceName`). The
 * product page provides this index so that every sourcing tooltip can show the
 * licence and the link of a source without prop drilling.
 */
export type ProductSourcesIndex = ReadonlyMap<string, ProductSourceDto>

export const productSourcesKey: InjectionKey<Ref<ProductSourcesIndex>> =
  Symbol('product-sources')

const normaliseSourceName = (name: string) => name.trim().toLowerCase()

/**
 * Index the sources by normalised name; sources without a name are skipped.
 */
export const indexProductSources = (
  sources: readonly ProductSourceDto[] | null | undefined
): ProductSourcesIndex => {
  const index = new Map<string, ProductSourceDto>()
  for (const source of sources ?? []) {
    const name = source?.name?.trim()
    if (name && !index.has(normaliseSourceName(name))) {
      index.set(normaliseSourceName(name), source)
    }
  }
  return index
}

/**
 * Provide the sources of the product to the descendant components.
 */
export const provideProductSources = (
  sources: () => readonly ProductSourceDto[] | null | undefined
) => {
  const index = computed(() => indexProductSources(sources()))
  provide(productSourcesKey, index)
  return index
}

/**
 * Resolve a source by name from the provided product sources. Outside a product
 * page (nothing provided) every lookup returns `undefined`.
 */
export const useProductSources = () => {
  const index = inject(productSourcesKey, null)

  return (name: string | null | undefined): ProductSourceDto | undefined => {
    if (!index || typeof name !== 'string' || !name.trim()) {
      return undefined
    }
    return index.value.get(normaliseSourceName(name))
  }
}
