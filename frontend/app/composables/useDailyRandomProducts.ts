import type { ProductDto } from '~~/shared/api-client'

const STORAGE_KEY = 'open4goods-daily-random-products'

type StoredDailyProducts = {
  date: string
  products: ProductDto[]
}

export const useDailyRandomProducts = () => {
  const dailyProducts = useState<ProductDto[]>(
    'daily-random-products',
    () => []
  )

  // Initialize on client side
  const initDailyProducts = async () => {
    if (import.meta.server) return

    const today = new Date().toISOString().split('T')[0]

    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const parsed: StoredDailyProducts = JSON.parse(stored)
        if (parsed.date === today && parsed.products.length > 0) {
          dailyProducts.value = parsed.products
          return
        }
      }

      // Fetch new products if expired or missing
      const products = await $fetch<ProductDto[]>('/api/stats/random', {
        query: {
          num: 10,
          minOffersCount: 3,
        },
      })

      if (products && products.length > 0) {
        // Ensure it's an array (api might return single object if not handled, but our api wrapper returns array or we fixed it in random.get.ts to return array)
        // Checking random.get.ts, it returns ProductDto[]

        const data: StoredDailyProducts = {
          date: today,
          products,
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        dailyProducts.value = products
      }
    } catch (e) {
      console.error('Failed to init daily random products', e)
    }
  }

  const getRandomProduct = () => {
    if (!dailyProducts.value || dailyProducts.value.length === 0) return null

    const randomIndex = Math.floor(Math.random() * dailyProducts.value.length)
    return dailyProducts.value[randomIndex]
  }

  return {
    dailyProducts,
    initDailyProducts,
    getRandomProduct,
  }
}
