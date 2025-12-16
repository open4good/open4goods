import { mount } from '@vue/test-utils'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { defineComponent, h, nextTick, ref } from 'vue'
import { createI18n } from 'vue-i18n'
import PageLoadingOverlay from './PageLoadingOverlay.vue'

const routeLoading = ref(false)

vi.mock('#imports', () => ({
  useState: vi.fn(() => routeLoading),
}))

const stubs = {
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
    attachTo: document.body,
  })

const findOverlay = () =>
  document.body.querySelector('[data-testid="page-loading-overlay"]')
const findSpinner = () =>
  document.body.querySelector('[data-testid="page-loading-spinner"]')
const flushOverlay = () => nextTick()

describe('PageLoadingOverlay', () => {
  beforeEach(() => {
    routeLoading.value = false
    document.documentElement.style.overflow = ''
    findOverlay()?.remove()
    findSpinner()?.remove()
  })

  it('renders the spinner when a route is loading', async () => {
    const wrapper = mountOverlay()

    routeLoading.value = true
    await flushOverlay()

    const overlay = findOverlay()
    expect(overlay).not.toBeNull()
    expect(overlay?.getAttribute('aria-live')).toBe('polite')

    const spinner = findSpinner()
    expect(spinner).not.toBeNull()
    expect(spinner?.getAttribute('aria-label')).toBe('Loading page content')

    wrapper.unmount()
  })

  it('removes the overlay after unmounting', async () => {
    const wrapper = mountOverlay()

    routeLoading.value = true
    await flushOverlay()
    routeLoading.value = false
    await flushOverlay()

    wrapper.unmount()
    await flushOverlay()

    expect(findOverlay()).toBeNull()
  })

  it('locks and restores document scrolling around unmount', async () => {
    const wrapper = mountOverlay()

    routeLoading.value = true
    await flushOverlay()
    expect(document.documentElement.style.overflow).toBe('hidden')

    wrapper.unmount()
    await flushOverlay()
    expect(document.documentElement.style.overflow).toBe('')
  })
})
