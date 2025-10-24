import { useLocalStorage } from '@vueuse/core'
import { computed } from 'vue'
import { defineStore } from 'pinia'
import type { ProductDto } from '~~/shared/api-client'

export const MAX_COMPARE_ITEMS = 4
const STORAGE_KEY = 'open4goods:compare-list'
const COLLAPSE_STORAGE_KEY = `${STORAGE_KEY}:collapsed`

export type CompareListBlockReason =
  | 'limit-reached'
  | 'vertical-mismatch'
  | 'missing-identifier'

export interface CompareListItem {
  id: string
  gtin?: number
  slug?: string
  fullSlug?: string
  verticalId?: string | null
  name: string
  image?: string
}

export interface CompareListActionResult {
  success: boolean
  reason?: CompareListBlockReason
}

const getProductIdentifier = (product: ProductDto): string | null => {
  if (product.gtin != null) {
    return String(product.gtin)
  }

  if (product.slug) {
    return product.slug
  }

  if (product.fullSlug) {
    return product.fullSlug
  }

  const fallbackParts = [
    product.identity?.bestName,
    product.identity?.model,
    product.identity?.brand,
    product.base?.bestName,
  ].filter((part): part is string => Boolean(part))

  if (fallbackParts.length > 0) {
    return fallbackParts.join('|')
  }

  return null
}

const resolveProductName = (product: ProductDto): string => {
  return (
    product.identity?.bestName ??
    product.base?.bestName ??
    product.identity?.model ??
    product.identity?.brand ??
    product.names?.h1Title ??
    product.names?.longestOfferName ??
    '#'
  )
}

const resolveProductImage = (product: ProductDto): string | undefined => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const resolveProductVertical = (product: ProductDto): string | null => {
  return product.base?.vertical ?? null
}

export const useProductCompareStore = defineStore('product-compare', () => {
  const items = useLocalStorage<CompareListItem[]>(STORAGE_KEY, [], {
    deep: true,
  })
  const isCollapsed = useLocalStorage<boolean>(COLLAPSE_STORAGE_KEY, false)

  const referenceVerticalId = computed(() => {
    return items.value.find((item) => item.verticalId)?.verticalId ?? null
  })

  const hasReachedLimit = computed(() => items.value.length >= MAX_COMPARE_ITEMS)

  const hasProduct = (product: ProductDto) => {
    const identifier = getProductIdentifier(product)

    if (!identifier) {
      return false
    }

    return items.value.some((item) => item.id === identifier)
  }

  const removeById = (id: string) => {
    items.value = items.value.filter((item) => item.id !== id)

    if (items.value.length === 0) {
      isCollapsed.value = false
    }
  }

  const removeProduct = (product: ProductDto) => {
    const identifier = getProductIdentifier(product)

    if (!identifier) {
      return
    }

    removeById(identifier)
  }

  const canAddProduct = (product: ProductDto): CompareListActionResult => {
    if (hasProduct(product)) {
      return { success: true }
    }

    if (hasReachedLimit.value) {
      return { success: false, reason: 'limit-reached' }
    }

    const productIdentifier = getProductIdentifier(product)

    if (!productIdentifier) {
      return { success: false, reason: 'missing-identifier' }
    }

    const productVertical = resolveProductVertical(product)
    const currentVertical = referenceVerticalId.value

    if (currentVertical && productVertical && currentVertical !== productVertical) {
      return { success: false, reason: 'vertical-mismatch' }
    }

    if (currentVertical && !productVertical) {
      return { success: false, reason: 'vertical-mismatch' }
    }

    return { success: true }
  }

  const addProduct = (product: ProductDto): CompareListActionResult => {
    const eligibility = canAddProduct(product)

    if (!eligibility.success) {
      return eligibility
    }

    const identifier = getProductIdentifier(product)

    if (!identifier) {
      return { success: false, reason: 'missing-identifier' }
    }

    if (hasProduct(product)) {
      return { success: true }
    }

    const newItem: CompareListItem = {
      id: identifier,
      gtin: product.gtin,
      slug: product.slug,
      fullSlug: product.fullSlug,
      verticalId: resolveProductVertical(product),
      name: resolveProductName(product),
      image: resolveProductImage(product),
    }

    items.value = [...items.value, newItem]
    isCollapsed.value = false

    return { success: true }
  }

  const toggleProduct = (product: ProductDto): CompareListActionResult => {
    if (hasProduct(product)) {
      removeProduct(product)
      return { success: true }
    }

    return addProduct(product)
  }

  const clear = () => {
    items.value = []
    isCollapsed.value = false
  }

  return {
    items,
    isCollapsed,
    hasReachedLimit,
    referenceVerticalId,
    hasProduct,
    canAddProduct,
    addProduct,
    toggleProduct,
    removeProduct,
    removeById,
    clear,
  }
})
