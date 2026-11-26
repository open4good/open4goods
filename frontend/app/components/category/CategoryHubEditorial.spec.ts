import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

import CategoryHubEditorial from './CategoryHubEditorial.vue'
import type { ProductDto } from '~~/shared/api-client'
import type { CategoryHubDto } from '~~/shared/utils/category-hub'

const asyncDataState = vi.hoisted(() => ({
  value: null as { hub: CategoryHubDto; lastScoredAt: number | null } | null,
}))
const headInputs = vi.hoisted(() => [] as unknown[])

mockNuxtImport('useAsyncData', () => async () => ({
  data: ref(asyncDataState.value),
  error: ref(null),
}))
mockNuxtImport('useHead', () => (input: unknown) => {
  headInputs.push(input)
})

const faq = [1, 2, 3, 4].map(index => ({
  question: `Question ${index} ?`,
  answer: `Réponse ${index}.`,
}))

const hub = (overrides: Partial<CategoryHubDto> = {}): CategoryHubDto => ({
  verticalId: 'tv',
  language: 'fr',
  published: true,
  title: null,
  introHtml: '<p>Une intro <strong>éditoriale</strong>.</p>',
  faq,
  subCategories: ['televiseur-55-pouces', 'tv_65_inches', 'inconnue'],
  guides: [
    { slug: 'guide-existant', label: 'Guide existant' },
    { slug: 'guide-absent', label: 'Guide absent' },
  ],
  ...overrides,
})

const product = (index: number, score: number | null): ProductDto =>
  ({
    gtin: 1000 + index,
    fullSlug: `/televiseurs/${1000 + index}-tv-${index}`,
    identity: { bestName: `TV ${index}` },
    base: { bestName: `TV ${index}` },
    scores: {
      scores: score == null ? {} : { ECOSCORE: { value: score } },
    },
  }) as unknown as ProductDto

const mountHub = () =>
  mountSuspended(CategoryHubEditorial, {
    props: {
      verticalId: 'tv',
      verticalHomeUrl: 'televiseurs',
      categoryName: 'Téléviseurs',
      subCategories: [
        {
          id: 'tv_55_inches',
          slug: 'televiseur-55-pouces',
          h1Title: 'TV 55 pouces',
        },
        {
          id: 'tv_65_inches',
          slug: 'televiseur-65-pouces',
          h1Title: 'TV 65 pouces',
        },
      ],
      guides: ['guide-existant'],
      rankedProducts: [
        ...[18, 17, 16, 15, 14, 13].map((score, index) =>
          product(index + 1, score)
        ),
        product(9, null),
      ],
    },
  })

// The last script list handed to useHead by the component.
const lastHeadScripts = () => {
  const input = headInputs.at(-1) as
    (() => { script: Array<{ type: string; innerHTML: string }> }) | undefined
  return input ? input().script : []
}

describe('CategoryHubEditorial', () => {
  beforeEach(() => {
    headInputs.length = 0
    asyncDataState.value = null
  })

  it('renders nothing and no JSON-LD when the vertical has no hub file', async () => {
    const wrapper = await mountHub()

    expect(wrapper.find('.category-hub').exists()).toBe(false)
    expect(lastHeadScripts()).toEqual([])
  })

  it('renders the intro, the 4 questions expanded, the dated Top 5 and the existing links', async () => {
    asyncDataState.value = {
      hub: hub(),
      lastScoredAt: Date.UTC(2026, 10, 24, 23, 30),
    }
    const wrapper = await mountHub()

    expect(wrapper.find('.category-hub__intro strong').text()).toBe(
      'éditoriale'
    )
    const questions = wrapper.findAll('.category-hub__faq-question')
    expect(questions.map(question => question.text())).toEqual(
      faq.map(item => item.question)
    )
    expect(wrapper.findAll('.category-hub__faq-answer')).toHaveLength(4)

    const top = wrapper.findAll('.category-hub__top-item')
    expect(top).toHaveLength(5)
    expect(top[0]!.find('a').attributes('href')).toBe('/televiseurs/1001-tv-1')
    // The date is the API timestamp, formatted in UTC (24 November, not 25).
    expect(wrapper.find('.category-hub__top-date').text()).toContain('24')

    const links = wrapper
      .findAll('.category-hub__links a')
      .map(link => link.attributes('href'))
    expect(links).toEqual([
      '/televiseurs/televiseur-55-pouces',
      '/televiseurs/televiseur-65-pouces',
      '/televiseurs/guide-existant',
    ])
    expect(wrapper.find('.category-hub__preview').exists()).toBe(false)
  })

  it('declares a FAQPage JSON-LD built from the visible questions', async () => {
    asyncDataState.value = { hub: hub(), lastScoredAt: null }
    const wrapper = await mountHub()

    const scripts = lastHeadScripts()
    expect(scripts).toHaveLength(1)
    expect(scripts[0]!.type).toBe('application/ld+json')
    const jsonLd = JSON.parse(scripts[0]!.innerHTML)
    expect(jsonLd['@type']).toBe('FAQPage')
    expect(jsonLd.mainEntity).toHaveLength(4)
    expect(
      jsonLd.mainEntity.map((entity: { name: string }) => entity.name)
    ).toEqual(
      wrapper.findAll('.category-hub__faq-question').map(node => node.text())
    )
    expect(jsonLd.mainEntity[0].acceptedAnswer).toEqual({
      '@type': 'Answer',
      text: 'Réponse 1.',
    })
    // Without the API date, the ranking stays undated: no date is built client side.
    expect(wrapper.find('.category-hub__top-date').exists()).toBe(false)
  })

  it('flags an unpublished hub served in preview', async () => {
    asyncDataState.value = {
      hub: hub({ published: false }),
      lastScoredAt: null,
    }
    const wrapper = await mountHub()

    expect(wrapper.find('.category-hub__preview').exists()).toBe(true)
  })
})
