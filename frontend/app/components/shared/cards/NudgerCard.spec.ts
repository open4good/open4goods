import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { defineComponent, h } from 'vue'
import NudgerCard from './NudgerCard.vue'

const createStub = (tag: string, className = '') =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_, { slots, attrs }) {
      return () =>
        h(
          tag,
          { class: [className, attrs.class], ...attrs },
          slots.default ? slots.default() : []
        )
    },
  })

const mountComponent = (props = {}) =>
  mountSuspended(NudgerCard, {
    props,
    global: {
      stubs: {
        VSheet: createStub('div', 'v-sheet-stub'),
      },
    },
  })

describe('NudgerCard', () => {
  it('sets custom corner radii when configured', async () => {
    const wrapper = await mountComponent({
      accentCorners: ['bottom-left'],
      flatCorners: ['top-left'],
    })

    const style = wrapper.attributes('style')
    expect(style).toContain('--nudger-card-bottom-left: 50px;')
    expect(style).toContain('--nudger-card-top-left: 0;')
    await wrapper.unmount()
  })

  it('applies the border class when enabled', async () => {
    const wrapper = await mountComponent({ border: true })

    expect(wrapper.classes()).toContain('nudger-card--border')
    await wrapper.unmount()
  })
})
