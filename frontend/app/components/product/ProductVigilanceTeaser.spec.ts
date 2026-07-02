import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ProductVigilanceTeaser from './ProductVigilanceTeaser.vue'

describe('ProductVigilanceTeaser', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          vigilance: {
            teaser: {
              alertCount:
                '{count} vigilance point identified | {count} vigilance points identified',
              cta: 'View vigilance points',
            },
            conflicts: {
              title: 'Conflicting Information',
            },
          },
        },
      },
    },
  })

  const createWrapper = async (product: ProductDto) => {
    return mountSuspended(ProductVigilanceTeaser, {
      props: {
        product,
      },
      global: {
        plugins: [i18n],
        stubs: {
          'v-icon': defineComponent({
            name: 'VIconStub',
            props: { icon: { type: String, default: '' } },
            setup(props) {
              return () => h('span', { class: 'v-icon-stub' }, props.icon)
            },
          }),
          'v-btn': defineComponent({
            name: 'VBtnStub',
            setup(_, { slots, attrs }) {
              return () =>
                h(
                  'button',
                  {
                    class: ['v-btn-stub', attrs.class],
                    type: 'button',
                    onClick: attrs.onClick as
                      ((event: MouseEvent) => void) | undefined,
                  },
                  slots.default?.()
                )
            },
          }),
          'v-chip': defineComponent({
            name: 'VChipStub',
            props: { color: { type: String, default: '' } },
            setup(props, { slots }) {
              return () =>
                h(
                  'span',
                  { class: ['v-chip-stub', props.color] },
                  slots.default?.()
                )
            },
          }),
        },
      },
    })
  }

  it('renders nothing when there are no alerts', async () => {
    const wrapper = await createWrapper({} as ProductDto)
    expect(wrapper.find('.product-vigilance-teaser').exists()).toBe(false)
    await wrapper.unmount()
  })

  it('renders teaser with alert count and chips when alerts exist', async () => {
    const wrapper = await createWrapper({
      attributes: {
        allAttributes: {
          attr1: {
            sourcing: {
              conflicts: true,
            },
          },
        },
      },
    } as ProductDto)

    expect(wrapper.text()).toContain('1 vigilance point identified')
    expect(wrapper.text()).toContain('View vigilance points')
    expect(wrapper.text()).toContain('Conflicting Information')

    await wrapper.unmount()
  })

  it('emits click:alerts event on CTA button click', async () => {
    const wrapper = await createWrapper({
      attributes: {
        allAttributes: {
          attr1: {
            sourcing: {
              conflicts: true,
            },
          },
        },
      },
    } as ProductDto)

    const button = wrapper.find('.product-vigilance-teaser__cta')
    await button.trigger('click')

    expect(wrapper.emitted('click:alerts')).toBeTruthy()
    expect(wrapper.emitted('click:alerts')?.[0]).toEqual([])

    await wrapper.unmount()
  })
})
