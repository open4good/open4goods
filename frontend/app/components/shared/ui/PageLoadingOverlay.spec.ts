import { mount } from '@vue/test-utils'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { computed, defineComponent, h, nextTick, ref } from 'vue'
import { createI18n } from 'vue-i18n'
import PageLoadingOverlay from './PageLoadingOverlay.vue'

const routeLoading = ref(false)

vi.mock('#imports', () => ({
  useState: vi.fn(() => routeLoading),
}))

vi.mock('~~/app/composables/useThemedAsset', () => ({
  useThemeAsset: () => computed(() => '/themed/launcher-icon.svg'),
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
  VAvatar: defineComponent({
    name: 'VAvatarStub',
    setup(_, { slots, attrs }) {
      return () =>
        h(
          'div',
          { ...attrs, 'data-testid': 'page-loading-icon-wrapper' },
          slots.default ? slots.default() : []
        )
    },
  }),
  VImg: defineComponent({
    name: 'VImgStub',
    props: {
      src: { type: String, default: '' },
      alt: { type: String, default: '' },
    },
    setup(props, { attrs }) {
      return () =>
        h('img', {
          ...attrs,
          'data-testid': 'page-loading-icon',
          src: props.src,
          alt: props.alt,
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
const findIcon = () =>
  document.body.querySelector('[data-testid="page-loading-icon"]')
const findSpinner = () =>
  document.body.querySelector('[data-testid="page-loading-spinner"]')
const flushOverlay = () => nextTick()

describe('PageLoadingOverlay', () => {
  beforeEach(() => {
    routeLoading.value = false
    document.documentElement.style.overflow = ''
    findOverlay()?.remove()
    findIcon()?.remove()
    document
      .querySelector('[data-testid="page-loading-icon-wrapper"]')
      ?.remove()
    findSpinner()?.remove()
  })

  it('renders the launcher icon when a route is loading', async () => {
    const wrapper = mountOverlay()

    routeLoading.value = true
    await flushOverlay()

    const overlay = findOverlay()
    expect(overlay).not.toBeNull()
    expect(overlay?.getAttribute('aria-live')).toBe('polite')

    const icon = findIcon()
    expect(icon).not.toBeNull()
    expect(icon?.getAttribute('alt')).toBe('Loading page content')
    expect(findSpinner()).toBeNull()

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
