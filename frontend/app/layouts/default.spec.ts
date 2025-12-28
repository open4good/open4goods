import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import DefaultLayout from './default.vue'

const drawerState = ref(false)

vi.mock('#imports', () => ({
  useState: vi.fn(() => drawerState),
  useRoute: vi.fn(() => ({ name: 'home' })),
  useDevice: vi.fn(() => ({ isMobileOrTablet: false })),
  useDisplay: vi.fn(() => ({ mdAndDown: { value: false } })),
}))

vi.mock('vuetify/lib/framework.mjs', () => ({
  useDisplay: () => ({ mdAndDown: { value: false } }),
  DisplaySymbol: Symbol.for('vuetify:display'),
}))

const stubs = {
  ClientOnly: {
    template: '<div><slot /></div>',
  },
  PageLoadingOverlay: {
    template: '<div data-testid="loading-overlay" />',
  },
  TheMainMenuContainer: {
    template: '<div data-testid="main-menu" />',
  },
  TheMainFooter: {
    template: '<footer><slot /></footer>',
  },
  TheMainFooterContent: {
    template: '<div data-testid="footer-content" />',
  },
  CategoryComparePanel: {
    template: '<div data-testid="compare-panel" />',
  },
  PwaOfflineNotice: {
    template: '<div data-testid="offline-notice" />',
  },
  PwaInstallPrompt: {
    template: '<div data-testid="install-prompt" />',
  },
  VApp: {
    template: '<div><slot /></div>',
  },
  VMain: {
    template: '<main data-testid="layout-main" v-bind="$attrs"><slot /></main>',
  },
  VNavigationDrawer: {
    template: '<aside><slot /></aside>',
  },
}

describe('default layout', () => {
  it('applies layout padding class to the main area', () => {
    const wrapper = mount(DefaultLayout, {
      global: {
        stubs,
      },
      slots: {
        default: '<div data-testid="page-slot" />',
      },
    })

    const main = wrapper.get('[data-testid="layout-main"]')

    expect(main.classes()).toContain('layout-main-content')
  })
})
