import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import enUS from '../../../i18n/locales/en-US.json'
import frFR from '../../../i18n/locales/fr-FR.json'

type MessageTree = { [key: string]: string | MessageTree }

const flatten = (tree: MessageTree, prefix = ''): Record<string, string> =>
  Object.entries(tree).reduce<Record<string, string>>((acc, [key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    if (typeof value === 'string') {
      acc[path] = value
    } else {
      Object.assign(acc, flatten(value, path))
    }
    return acc
  }, {})

const frMessages = flatten(
  (frFR as unknown as { rankingPolicy: MessageTree }).rankingPolicy,
  'rankingPolicy'
)
const enMessages = flatten(
  (enUS as unknown as { rankingPolicy: MessageTree }).rankingPolicy,
  'rankingPolicy'
)

const translate = (key: string, params?: Record<string, unknown>) => {
  const message = frMessages[key] ?? key
  return params?.date ? message.replace('{date}', String(params.date)) : message
}

mockNuxtImport('useI18n', () => () => ({
  t: translate,
  locale: { value: 'fr-FR' },
}))
mockNuxtImport('useRequestURL', () => () => new URL('https://nudger.test/'))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useHead', () => vi.fn())

const PageHeaderStub = defineComponent({
  name: 'PageHeader',
  props: { title: { type: String, default: '' } },
  setup(props) {
    return () => h('header', { class: 'page-header-stub' }, props.title)
  },
})

const NuxtLinkStub = defineComponent({
  name: 'NuxtLink',
  props: { to: { type: String, default: '' } },
  setup(props, { slots }) {
    return () => h('a', { href: props.to }, slots.default?.())
  },
})

const mountPage = async () => {
  const component = (await import('./index.vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
        PageHeader: PageHeaderStub,
        NuxtLink: NuxtLinkStub,
      },
    },
  })
}

describe('ranking and listing page (GOU-1995)', () => {
  it('renders every section with its translated text', async () => {
    const wrapper = await mountPage()

    for (const key of [
      'referencing',
      'remuneration',
      'defaultSort',
      'otherSorts',
      'search',
      'offers',
      'alternatives',
    ]) {
      const section = wrapper.find(
        `[data-testid="ranking-policy-section-${key}"]`
      )
      expect(section.exists()).toBe(true)
      expect(section.text()).toContain(
        frMessages[`rankingPolicy.sections.${key}.title`]
      )
    }

    expect(wrapper.text()).not.toContain('rankingPolicy.')
  })

  it('shows the update date and links to the Impact Score method and contact', async () => {
    const wrapper = await mountPage()

    expect(
      wrapper.find('[data-testid="ranking-policy-updated"]').text()
    ).toContain('9 novembre 2026')

    const hrefs = wrapper.findAll('a').map(link => link.attributes('href'))
    expect(hrefs).toContain('/impact-score')
    expect(hrefs).toContain('/contact')
  })

  it('has the same keys in fr-FR and en-US', () => {
    expect(Object.keys(enMessages).sort()).toEqual(
      Object.keys(frMessages).sort()
    )
    for (const value of [
      ...Object.values(frMessages),
      ...Object.values(enMessages),
    ]) {
      expect(value.trim()).not.toBe('')
    }
  })

  it('keeps the public text free of generic environmental claims', () => {
    const text = [
      ...Object.values(frMessages),
      ...Object.values(enMessages),
    ].join(' ')

    expect(text).not.toMatch(/[ée]cologique|responsable|eco-friendly|green/i)
  })
})
