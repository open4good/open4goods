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
 *
 * Only a source that declares a licence or an attribution is credited (its
 * link, licence and attribution shown); any other source keeps its name alone,
 * so no merchant link and no "unknown licence" ever appears.
 */
export type ProductSourcesIndex = ReadonlyMap<string, ProductSourceDto>

export const productSourcesKey: InjectionKey<Ref<ProductSourcesIndex>> =
  Symbol('product-sources')

const normaliseSourceName = (name: string) => name.trim().toLowerCase()

const hasText = (value: string | null | undefined) =>
  typeof value === 'string' && value.trim().length > 0

/**
 * Whether the source declares a licence or an attribution text, the only
 * sources whose credit (link, licence, attribution) is displayed.
 */
export const declaresAttribution = (
  source: ProductSourceDto | null | undefined
): source is ProductSourceDto =>
  Boolean(source) && (hasText(source?.license) || hasText(source?.attribution))

/**
 * The sources to credit on the product page: those declaring a licence or an
 * attribution, in their original order.
 */
export const attributedProductSources = (
  sources: readonly ProductSourceDto[] | null | undefined
): ProductSourceDto[] =>
  (sources ?? []).filter(
    source => declaresAttribution(source) && hasText(source.name)
  )

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
 * page (nothing provided) every lookup returns `undefined`. Callers decide what
 * to show with {@link declaresAttribution}.
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
