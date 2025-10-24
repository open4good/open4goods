import { mount } from '@vue/test-utils'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { defineComponent, h, nextTick, ref, watchEffect } from 'vue'
import { createI18n } from 'vue-i18n'
import PageLoadingOverlay from './PageLoadingOverlay.vue'

const routeLoading = ref(false)
const overlayStates: boolean[] = []

vi.mock('#imports', () => ({
  useState: vi.fn(() => routeLoading),
}))

const stubs = {
  teleport: true,
  VOverlay: defineComponent({
    name: 'VOverlayStub',
    setup(_, { slots, attrs }) {
      watchEffect(() => {
        overlayStates.push(routeLoading.value)
      })

      return () => (routeLoading.value ? h('div', attrs, slots.default?.()) : null)
    },
  }),
  VProgressCircular: defineComponent({
    name: 'VProgressCircularStub',
    props: {
      ariaLabel: {
        type: String,
        default: undefined,
      },
    },
    setup(props, { attrs }) {
      return () =>
        h('div', {
          ...attrs,
          'data-testid': 'page-loading-spinner',
          'aria-label': props.ariaLabel,
        })
    },
  }),
}

const createI18nPlugin = () =>
  createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        components: {
          pageLoadingOverlay: {
            label: 'Loading page content',
          },
        },
      },
    },
  })

const mountOverlay = () =>
  mount(PageLoadingOverlay, {
    global: {
      stubs,
      plugins: [createI18nPlugin()],
    },
  })

describe('PageLoadingOverlay', () => {
  beforeEach(() => {
    routeLoading.value = false
    overlayStates.length = 0
  })

  it('renders the spinner when a route is loading', async () => {
    const wrapper = mountOverlay()

    routeLoading.value = true
    await nextTick()

    const spinner = wrapper.find('[data-testid="page-loading-spinner"]')
    expect(spinner.exists()).toBe(true)
    expect(spinner.attributes('aria-label')).toBe('Loading page content')
    expect(overlayStates.includes(true)).toBe(true)

    wrapper.unmount()
  })

  it('disables the overlay when no route is loading', async () => {
    const wrapper = mountOverlay()

    await nextTick()

    expect(routeLoading.value).toBe(false)
    expect(overlayStates.at(-1)).toBe(false)
    expect(wrapper.find('[data-testid="page-loading-spinner"]').exists()).toBe(false)

    wrapper.unmount()
  })
})
