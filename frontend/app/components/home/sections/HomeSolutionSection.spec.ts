import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import HomeSolutionSection from './HomeSolutionSection.vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
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

const mountComponent = () =>
  mountSuspended(HomeSolutionSection, {
    props: {
      benefits: [
        {
          emoji: '🌿',
          label: 'label',
          description: 'description',
        },
      ],
    },
    global: {
      stubs: {
        VRow: createStub('div'),
        VCol: createStub('div'),
        VAvatar: createStub('div'),
        NudgerCard: createStub('div', 'nudger-card-stub'),
      },
    },
  })

describe('HomeSolutionSection', () => {
  it('unlocks the tilted image after pointer entry', async () => {
    const wrapper = await mountComponent()
    const image = wrapper.find('.home-solution__image')

    expect(image.classes()).toContain('home-tilt-lock')
    expect(image.classes()).not.toContain('home-tilt-lock--unlocked')

    await wrapper.find('.home-solution__image-wrapper').trigger('pointerenter')

    expect(image.classes()).toContain('home-tilt-lock--unlocked')

    await wrapper.unmount()
  })
})
