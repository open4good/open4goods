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

  it('applies shadow class by default', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.classes()).toContain('nudger-card--shadow')
    await wrapper.unmount()
  })

  it('omits shadow class when shadow is false', async () => {
    const wrapper = await mountComponent({ shadow: false })

    expect(wrapper.classes()).not.toContain('nudger-card--shadow')
    await wrapper.unmount()
  })

  it('applies hoverable class by default', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.classes()).toContain('nudger-card--hoverable')
    await wrapper.unmount()
  })

  it('omits hoverable class when hoverable is false', async () => {
    const wrapper = await mountComponent({ hoverable: false })

    expect(wrapper.classes()).not.toContain('nudger-card--hoverable')
    await wrapper.unmount()
  })

  it('uses individual corner radius overrides with highest priority', async () => {
    const wrapper = await mountComponent({
      accentCorners: ['top-left'],
      flatCorners: ['bottom-right'],
      topLeftRadius: '10px',
      bottomRightRadius: '20px',
    })

    const style = wrapper.attributes('style')
    // Individual overrides should take priority over accent/flat corners
    expect(style).toContain('--nudger-card-top-left: 10px;')
    expect(style).toContain('--nudger-card-bottom-right: 20px;')
    await wrapper.unmount()
  })

  it('applies individual corner radius without affecting other corners', async () => {
    const wrapper = await mountComponent({
      topRightRadius: '15px',
    })

    const style = wrapper.attributes('style')
    expect(style).toContain('--nudger-card-top-right: 15px;')
    // Other corners should use baseRadius (30px by default)
    expect(style).toContain('--nudger-card-top-left: 30px;')
    expect(style).toContain('--nudger-card-bottom-right: 30px;')
    expect(style).toContain('--nudger-card-bottom-left: 30px;')
    await wrapper.unmount()
  })
})
