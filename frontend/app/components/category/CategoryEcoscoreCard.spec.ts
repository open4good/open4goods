import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { defineComponent, h } from 'vue'

import CategoryEcoscoreCard from './CategoryEcoscoreCard.vue'

describe('CategoryEcoscoreCard', () => {
  const vuetify = createVuetify()
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        category: {
          filters: {
            ecoscore: {
              title: 'Discover',
              description: 'Learn more',
              cta: 'Read more',
              ariaLabel: 'Open Eco-score guide',
            },
          },
        },
      },
    },
  })

  const mountCard = (verticalHomeUrl?: string | null) =>
    mount(CategoryEcoscoreCard, {
      props: { verticalHomeUrl: verticalHomeUrl ?? null },
      global: {
        plugins: [vuetify, i18n],
        stubs: {
          VCard: defineComponent({
            name: 'VCardStub',
            inheritAttrs: false,
            props: {
              to: {
                type: [String, Object],
                default: undefined,
              },
            },
            setup(props, { attrs, slots }) {
              const resolveHref = () => {
                if (!props.to) {
                  return undefined
                }

                if (typeof props.to === 'string') {
                  return props.to
                }

                if (typeof props.to === 'object') {
                  return (
                    (props.to as Record<string, string>).href ??
                    (props.to as Record<string, string>).path
                  )
                }

                return undefined
              }

              return () =>
                h(
                  'a',
                  {
                    ...attrs,
                    href: resolveHref(),
                  },
                  slots.default?.()
                )
            },
          }),
          VIcon: defineComponent({
            name: 'VIconStub',
            props: {
              icon: {
                type: String,
                default: '',
              },
              size: {
                type: [String, Number],
                default: 24,
              },
            },
            setup(props) {
              return () => h('span', { 'data-icon': props.icon })
            },
          }),
        },
      },
    })

  it('renders a link to the Eco-score page when the base URL is provided', () => {
    const wrapper = mountCard('https://example.com/vertical')

    const link = wrapper.get('[data-test="category-ecoscore-card"]')
    expect(link.attributes('href')).toBe(
      'https://example.com/vertical/ecoscore'
    )
  })

  it('normalizes trailing slashes when computing the Eco-score link', () => {
    const wrapper = mountCard('https://example.com/vertical/')

    const link = wrapper.get('[data-test="category-ecoscore-card"]')
    expect(link.attributes('href')).toBe(
      'https://example.com/vertical/ecoscore'
    )
  })

  it('does not render anything when the base URL is missing', () => {
    const wrapper = mountCard(null)

    expect(wrapper.find('[data-test="category-ecoscore-card"]').exists()).toBe(
      false
    )
  })
})
