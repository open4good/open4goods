import { flushPromises, mount } from '@vue/test-utils'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import PwaBarcodeScanner from './PwaBarcodeScanner.vue'

mockNuxtImport('useDevice', () => () => ({
  isMobileOrTablet: true,
}))

vi.mock('vue-barcode-reader', () => ({
  StreamBarcodeReader: defineComponent({
    name: 'StreamBarcodeReaderStub',
    emits: ['decode'],
    setup(_, { emit }) {
      return () =>
        h(
          'div',
          {
            class: 'stream-barcode-reader-stub',
            onClick: () => emit('decode', '0123456789012'),
          },
          []
        )
    },
  }),
}))

const mountScanner = () =>
  mount(PwaBarcodeScanner, {
    props: {
      active: true,
      loadingLabel: 'Loading…',
      errorLabel: 'Camera unavailable.',
    },
    global: {
      stubs: {
        VProgressCircular: defineComponent({
          name: 'VProgressCircularStub',
          setup(_, { slots }) {
            return () =>
              h(
                'div',
                { class: 'v-progress-circular-stub' },
                slots.default ? slots.default() : []
              )
          },
        }),
      },
    },
  })

describe('PwaBarcodeScanner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the fallback scanner when native APIs are missing', async () => {
    const wrapper = mountScanner()
    await flushPromises()

    const viewport = wrapper.find(
      '[data-test="pwa-barcode-scanner"] .pwa-barcode-scanner__viewport'
    )
    expect(viewport.attributes()['data-mode']).toBe('fallback')
    expect(wrapper.find('[data-test="pwa-barcode-fallback"]').exists()).toBe(
      true
    )
  })

  it('emits decoded values from the fallback component', async () => {
    const wrapper = mountScanner()
    await flushPromises()

    const fallback = wrapper.findComponent({ name: 'StreamBarcodeReaderStub' })
    fallback.vm.$emit('decode', ' 5901234123457 ')

    expect(wrapper.emitted('decode')?.[0]).toEqual(['5901234123457'])
  })
})
