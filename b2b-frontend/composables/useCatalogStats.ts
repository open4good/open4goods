interface CatalogStats {
  indexedProducts: number
}

const FALLBACK_COUNT = 83_000_000

export function useCatalogStats() {
  const { data, error } = useFetch<CatalogStats>('/api/backend/api/v1/catalog/stats', {
    server: true,
    lazy: false,
    default: () => ({ indexedProducts: FALLBACK_COUNT })
  })

  const indexedProducts = computed(() => data.value?.indexedProducts ?? FALLBACK_COUNT)

  const indexedProductsFormatted = computed(() => {
    const n = indexedProducts.value
    if (n >= 1_000_000) {
      return `${(n / 1_000_000).toFixed(0)}M+`
    }
    if (n >= 1_000) {
      return `${(n / 1_000).toFixed(0)}K+`
    }
    return n.toLocaleString()
  })

  return { indexedProducts, indexedProductsFormatted, error }
}
