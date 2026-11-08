export interface CategoryItemListInput {
  name: string
  numberOfItems: number
  url: string
  productUrls: Array<string | null | undefined>
}

/**
 * ItemList "page de synthèse" : chaque ListItem ne porte que position + url.
 * Pas de Product/Offer imbriqué : Nudger compare, il ne vend pas.
 */
export const buildCategoryItemListJsonLd = (input: CategoryItemListInput) => {
  const itemListElement = input.productUrls
    .filter((url): url is string => typeof url === 'string' && url.length > 0)
    .map((url, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      url,
    }))

  if (!itemListElement.length) {
    return null
  }

  return {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    name: input.name,
    numberOfItems: input.numberOfItems,
    itemListOrder: 'https://schema.org/ItemListUnordered',
    url: input.url,
    itemListElement,
  }
}
