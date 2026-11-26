import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import type {
  AttributeConfigDto,
  ProductCardDto,
  ProductDto,
} from '~~/shared/api-client'
import ProductCard from '~/components/product/ProductCard.vue'
import CategoryProductListView from './CategoryProductListView.vue'
import CategoryProductTable from './CategoryProductTable.vue'

/**
 * GOU-2710: listings render from the lightweight `ProductCardDto`
 * (`POST /products/cards`) instead of the full `ProductDto`. Both fixtures are
 * the front-api test resources: a real television of `/televiseurs` and its
 * card, produced by `ProductCardMappingService` (golden file checked by
 * `ProductCardMappingServiceTest`). The card must render exactly like the full
 * product in every listing view.
 */
// Vitest runs from the frontend root; the fixtures live in the sibling module.
const FIXTURES_DIR = resolve(
  process.cwd(),
  '../front-api/src/test/resources/fixtures'
)

const readFixture = <T>(name: string): T =>
  JSON.parse(readFileSync(resolve(FIXTURES_DIR, name), 'utf8')) as T

const fullProduct = readFixture<ProductDto>('product-dto-tv-listing.json')
const cardProduct = readFixture<ProductCardDto>('product-card-tv-listing.json')

const popularAttributes: AttributeConfigDto[] = [
  {
    key: 'DIAGONALE_POUCES',
    name: 'Diagonale (en pouces)',
    suffix: '"',
    filteringType: 'NUMERIC',
  } as AttributeConfigDto,
  {
    key: 'DISPLAY_TECHNOLOGY',
    name: "Technologie d'affichage",
    filteringType: 'TEXT',
  } as AttributeConfigDto,
]

const tableAttributeKeys = [
  'DIAGONALE_POUCES',
  'DISPLAY_TECHNOLOGY',
  'HDMI_PORTS_QUANTITY',
  'REPAIRABILITY_INDEX',
  'CLASSE_ENERGY',
]

describe('listing card payload (GOU-2710)', () => {
  it('weighs less than 5 KB against about 50 KB for the full product', () => {
    expect(JSON.stringify(cardProduct).length).toBeLessThan(5_000)
    expect(JSON.stringify(fullProduct).length).toBeGreaterThan(40_000)
  })

  it.each([
    ['without sort', null],
    [
      'sorted by an attribute',
      'attributes.indexed.DIAGONALE_POUCES.numericValue',
    ],
    ['sorted by offers', 'offersCount'],
  ])('renders the same product card %s', async (_label, sortField) => {
    const render = async (product: ProductDto) =>
      (
        await mountSuspended(ProductCard, {
          props: { product, popularAttributes, sortField },
        })
      ).html()

    const fromCard = await render(cardProduct as ProductDto)

    expect(fromCard).toBe(await render(fullProduct))
    expect(fromCard).toContain('PHILIPS 40PFS6009/12')
    expect(fromCard).toContain('rueducommerce.fr')
  })

  it('renders the same list row', async () => {
    const render = async (product: ProductDto) =>
      (
        await mountSuspended(CategoryProductListView, {
          props: { products: [product], popularAttributes },
        })
      ).html()

    expect(await render(cardProduct as ProductDto)).toBe(
      await render(fullProduct)
    )
  })

  it('renders the same table row', async () => {
    const render = async (product: ProductDto) =>
      (
        await mountSuspended(CategoryProductTable, {
          props: {
            products: [product],
            itemsPerPage: 10,
            popularAttributes,
            attributeKeys: tableAttributeKeys,
          },
        })
      ).html()

    const fromCard = await render(cardProduct as ProductDto)

    expect(fromCard).toBe(await render(fullProduct))
    expect(fromCard).toContain('LED')
  })
})
