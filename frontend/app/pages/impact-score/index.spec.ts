import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'

import enUS from '../../../i18n/locales/en-US.json'
import frFR from '../../../i18n/locales/fr-FR.json'

type MessageTree = { [key: string]: string | MessageTree }

const lookup = (tree: MessageTree, key: string): string => {
  const value = key
    .split('.')
    .reduce<string | MessageTree | undefined>(
      (node, part) =>
        node && typeof node !== 'string' ? node[part] : undefined,
      tree
    )
  return typeof value === 'string' ? value : key
}

const state = vi.hoisted(() => ({ locale: 'fr-FR' }))

mockNuxtImport('useI18n', () => () => ({
  t: (key: string) =>
    lookup((state.locale === 'fr-FR' ? frFR : enUS) as MessageTree, key),
  locale: { value: state.locale },
}))
mockNuxtImport('useLocalePath', () => () => (path: string) => path)
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useAsyncData', () => async () => ({ data: { value: [] } }))

const stub = (name: string) =>
  defineComponent({ name, setup: () => () => h('div', { class: name }) })

const mountPage = async () => {
  const component = (await import('./index.vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
        ImpactScoreHero: stub('ImpactScoreHero'),
        ImpactScoreMethodology: stub('ImpactScoreMethodology'),
        ImpactScoreAvailableCriteria: stub('ImpactScoreAvailableCriteria'),
        ImpactScoreDocsPanel: stub('ImpactScoreDocsPanel'),
        ImpactScore: stub('ImpactScoreDemo'),
        NuxtLink: defineComponent({
          props: { to: { type: String, default: '' } },
          setup:
            (props, { slots }) =>
            () =>
              h('a', { href: props.to }, slots.default?.()),
        }),
      },
    },
  })
}

describe.each([
  ['fr-FR', frFR, "Ce que c'est"],
  ['en-US', enUS, 'What it is'],
])('/impact-score page (%s)', (locale, messages, firstTitle) => {
  it('renders the 5 blocks in order, with the example and the history link', async () => {
    state.locale = locale
    const wrapper = await mountPage()
    const ids = wrapper
      .findAll('section[id]')
      .map(section => section.attributes('id'))
    expect(ids).toEqual(['what', 'not', 'read', 'limits', 'history'])

    const titles = wrapper.findAll('h2').map(h2 => h2.text())
    expect(titles[0]).toBe(firstTitle)
    expect(wrapper.find('.ImpactScoreDemo').exists()).toBe(true)
    expect(wrapper.find('a[href="/impact-score/versions"]').exists()).toBe(true)

    const text = wrapper.text()
    const page = (messages as unknown as { impactScorePage: MessageTree })
      .impactScorePage
    expect(text).toContain(lookup(page, 'what.rating'))
    expect(text).toContain(
      lookup(messages as MessageTree, 'components.impactScore.v2.disclaimer')
    )
  })

  it('does not use removed wording', () => {
    const page = JSON.stringify(
      (messages as unknown as { impactScorePage: MessageTree }).impactScorePage
    )
    expect(page).not.toMatch(
      /éco-?score|eco-?score|vérifié|verified|La planète/i
    )
  })
})
