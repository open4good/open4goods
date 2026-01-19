export interface CategorySuggestionItem {
  type: 'category'
  id: string
  title: string
  image: string | null
  url: string | null
  verticalId: string | null
}

export interface ProductSuggestionItem {
  type: 'product'
  id: string
  title: string
  image: string | null
  gtin: string | null
  verticalId: string | null
  ecoscoreValue: number | null
  bestPrice: number | null
  bestPriceCurrency: string | null
}

export type SuggestionItem = CategorySuggestionItem | ProductSuggestionItem
