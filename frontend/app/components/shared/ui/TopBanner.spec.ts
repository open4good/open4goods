import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createVuetify } from 'vuetify'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'ui.topBanner.close': 'Close banner',
      }

      return translations[key] ?? key
    },
  }),
}))

describe('TopBanner', () => {
  const vuetify = createVuetify()

  const mountComponent = async (props: Record<string, unknown>) => {
    const module = await import('./TopBanner.vue')
    const TopBanner = module.default

    return mount(TopBanner, {
      props,
      global: {
        plugins: [vuetify],
        stubs: {
          VSheet: { template: '<section class="v-sheet"><slot /></section>' },
          VBtn: {
            template: '<button class="v-btn" @click="$emit(\'click\', $event)"><slot /></button>',
            props: ['color', 'variant', 'size', 'href', 'target', 'rel', 'ariaLabel'],
          },
          VIcon: { template: '<span class="v-icon"><slot /></span>' },
          VSlideYTransition: { template: '<div class="v-slide"><slot /></div>' },
        },
      },
    })
  }

  it('renders content when open', async () => {
    const wrapper = await mountComponent({
      open: true,
      message: 'Support an ethical platform!',
      ctaLabel: 'Shop with Nudger',
    })

    expect(wrapper.text()).toContain('Support an ethical platform!')
    expect(wrapper.text()).toContain('Shop with Nudger')
    expect(wrapper.emitted('show')).toBeTruthy()
  })

  it('emits hide when visibility turns off', async () => {
    const wrapper = await mountComponent({
      open: true,
      message: 'Banner message',
    })

    await wrapper.setProps({ open: false })

    expect(wrapper.emitted('hide')).toBeTruthy()
  })

  it('emits cta-click when CTA is pressed', async () => {
    const wrapper = await mountComponent({
      open: true,
      message: 'Banner message',
      ctaLabel: 'Go to price',
    })

    const ctaButton = wrapper.find('button.v-btn')
    expect(ctaButton.exists()).toBe(true)

    await ctaButton.trigger('click')
    expect(wrapper.emitted('cta-click')).toBeTruthy()
  })
})
