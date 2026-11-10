import { mountSuspended, registerEndpoint } from '@nuxt/test-utils/runtime'
import { describe, it, expect } from 'vitest'
import SearchPage from './index.vue'

const product = (gtin: number, name: string) => ({
  gtin,
  names: { h1Title: name, pageTitle: name },
})

const respond = (accessories: unknown[]) => ({
  verticalGroups: [],
  missingVerticalResults: [{ product: product(1, 'Téléviseur LG'), score: 1 }],
  missingVerticalPage: { totalElements: 1, totalPages: 1 },
  accessoryResults: accessories,
})

const mountWith = async (query: string, accessories: unknown[]) => {
  const calls: unknown[] = []
  registerEndpoint('/api/products/search', {
    method: 'POST',
    handler: async event => {
      const { readBody } = await import('h3')
      calls.push(await readBody(event))
      return respond(accessories)
    },
  })
  registerEndpoint('/api/stats/categories', () => ({}))
  const wrapper = await mountSuspended(SearchPage, {
    route: { path: '/rechercher', query: { q: query } },
    global: {
      stubs: {
        VNavigationDrawer: { template: '<div><slot /></div>' },
      },
    },
  })
  await new Promise(resolve => setTimeout(resolve, 50))
  return { wrapper, calls }
}

describe('search page', () => {
  it.each(['tv', 'lg'])(
    'lance la recherche pour « %s » (2 caractères)',
    async q => {
      const { wrapper, calls } = await mountWith(q, [])
      expect(calls.length).toBeGreaterThan(0)
      expect((calls[0] as { query: string }).query).toBe(q)
      expect(wrapper.html()).toContain('GTIN: 1')
    }
  )

  it('masque le bloc accessoires quand la liste est vide', async () => {
    const { wrapper } = await mountWith('lg oled55c3', [])
    expect(wrapper.find('[data-test="search-accessories"]').exists()).toBe(
      false
    )
  })

  it('affiche le bloc accessoires quand la liste est non vide', async () => {
    const { wrapper } = await mountWith('lg oled55c3', [
      { product: product(2, 'Support mural LG'), score: 1 },
    ])
    const block = wrapper.find('[data-test="search-accessories"]')
    expect(block.exists()).toBe(true)
    expect(block.text()).toContain('Accessoires et pièces détachées')
    expect(block.text()).toContain('1 produit')
  })
})
