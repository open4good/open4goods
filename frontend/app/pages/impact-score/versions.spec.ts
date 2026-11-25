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
mockNuxtImport('useHead', () => vi.fn())
mockNuxtImport('useCanonicalUrl', () => () => ({ value: null }))

const mountPage = async () => {
  const component = (await import('./versions.vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
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
  ['fr-FR', frFR],
  ['en-US', enUS],
])('/impact-score/versions page (%s)', (locale, messages) => {
  it('shows V2 in force since 16/11 and V1 alongside, without "not yet in force"', async () => {
    state.locale = locale
    const wrapper = await mountPage()
    const v2 = wrapper.find('#v2')
    expect(v2.exists()).toBe(true)
    expect(v2.find('time').attributes('datetime')).toBe('2026-11-16')
    expect(v2.text()).toContain(
      lookup(messages as MessageTree, 'impactScoreVersionsPage.partial')
    )
    expect(wrapper.find('#v1').exists()).toBe(true)
    expect(wrapper.text()).not.toMatch(
      /pas encore en vigueur|not yet in force|annoncée|announced/i
    )
  })
})
