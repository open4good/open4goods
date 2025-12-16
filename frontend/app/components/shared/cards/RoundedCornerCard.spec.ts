import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import RoundedCornerCard from './RoundedCornerCard.vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    tm: () => [],
  }),
}))

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
  mountSuspended(RoundedCornerCard, {
    props: {
      title: 'Sample',
      subtitle: 'Subtitle',
      ...props,
    },
    global: {
      stubs: {
        VCard: createStub('div', 'v-card-stub'),
        VIcon: createStub('div', 'v-icon-stub'),
        VImg: createStub('img', 'v-img-stub'),
        VProgressCircular: createStub('div', 'v-progress-circular-stub'),
        VBtn: createStub('button', 'v-btn-stub'),
      },
    },
  })

describe('RoundedCornerCard', () => {
  it('renders a text corner variant when configured', async () => {
    const wrapper = await mountComponent({
      cornerVariant: 'text',
      cornerLabel: 'Fresh',
    })

    expect(wrapper.find('.rounded-card__corner-label').text()).toContain(
      'Fresh'
    )
    await wrapper.unmount()
  })

  it('emits selection events when clicked', async () => {
    const wrapper = await mountComponent({
      cornerVariant: 'icon',
      selected: false,
    })

    await wrapper.trigger('click')

    expect(wrapper.emitted('update:selected')?.[0]).toEqual([true])
    expect(wrapper.emitted('select')?.[0]).toEqual([true])

    await wrapper.unmount()
  })
})
