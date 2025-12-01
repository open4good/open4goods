import { mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

const messages: Record<string, string> = {
  'pwa.install.title': 'Install Nudger',
  'pwa.install.description': 'Install Nudger for offline access.',
  'pwa.install.cta': 'Install',
  'pwa.install.dismiss': 'Dismiss',
  'pwa.install.updateTitle': 'Update available',
  'pwa.install.updateDescription': 'A new version is ready.',
  'pwa.install.updateCta': 'Reload',
  'pwa.install.offlineReady': 'Offline ready',
  'pwa.install.offlineReadyDescription': 'The app now works offline.',
  'pwa.install.success': 'Installed successfully.',
  'pwa.install.error': 'Installation failed.',
}

const installPromptVisible = ref(false)
const isInstallSupported = ref(false)
const installOutcome = ref<'accepted' | 'dismissed' | null>(null)
const installError = ref<string | null>(null)
const installInProgress = ref(false)
const offlineReady = ref(false)
const updateAvailable = ref(false)
const mobileBreakpoint = ref(false)

const dismissInstall = vi.fn()
const requestInstall = vi.fn()
const applyUpdate = vi.fn()

const resetState = () => {
  installPromptVisible.value = false
  isInstallSupported.value = false
  installOutcome.value = null
  installError.value = null
  installInProgress.value = false
  offlineReady.value = false
  updateAvailable.value = false
  mobileBreakpoint.value = false
  dismissInstall.mockReset()
  requestInstall.mockReset()
  applyUpdate.mockReset()
}

vi.mock('~~/app/composables/pwa/usePwaInstallPromptBridge', () => ({
  usePwaInstallPromptBridge: () => ({
    t: (key: string) => messages[key] ?? key,
    installPromptVisible,
    isInstallSupported,
    installOutcome,
    installError,
    installInProgress,
    offlineReady,
    updateAvailable,
    dismissInstall,
    requestInstall,
    applyUpdate,
  }),
}))

vi.mock('vuetify', () => ({
  useDisplay: () => ({
    smAndDown: mobileBreakpoint,
  }),
}))

type InstallPromptComponent = typeof import('./PwaInstallPrompt.vue')['default']
let PwaInstallPrompt: InstallPromptComponent

beforeAll(async () => {
  PwaInstallPrompt = (await import('./PwaInstallPrompt.vue')).default
})

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_, { slots }) {
    return () => (slots.default ? slots.default() : [])
  },
})

const VAlertStub = defineComponent({
  name: 'VAlertStub',
  props: {
    title: { type: String, default: '' },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'div',
        {
          class: 'v-alert-stub',
          ...attrs,
        },
        [
          props.title ? h('strong', { class: 'v-alert-stub__title' }, props.title) : null,
          slots.default ? h('div', { class: 'v-alert-stub__content' }, slots.default()) : null,
          slots.actions ? h('div', { class: 'v-alert-stub__actions' }, slots.actions()) : null,
        ],
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  emits: ['click'],
  setup(_, { slots, emit, attrs }) {
    return () =>
      h(
        'button',
        {
          type: 'button',
          class: 'v-btn-stub',
          ...attrs,
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default ? slots.default() : [],
      )
  },
})

const mountPrompt = () =>
  mount(PwaInstallPrompt, {
    global: {
      stubs: {
        VAlert: VAlertStub,
        VBtn: VBtnStub,
      },
      components: {
        ClientOnly: ClientOnlyStub,
      },
    },
  })

describe('PwaInstallPrompt', () => {
  beforeEach(() => {
    resetState()
  })

  it('hides the install banner on desktop displays', async () => {
    const wrapper = mountPrompt()
    installPromptVisible.value = true
    isInstallSupported.value = true
    await nextTick()

    expect(wrapper.find('[data-test="pwa-install-banner"]').exists()).toBe(false)
  })



  it('shows update and offline ready banners', async () => {
    const wrapper = mountPrompt()
    updateAvailable.value = true
    offlineReady.value = true
    await nextTick()

    expect(wrapper.find('[data-test="pwa-update-banner"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="pwa-offline-ready"]').exists()).toBe(true)
  })

  it('applies updates when requested', async () => {
    const wrapper = mountPrompt()
    updateAvailable.value = true
    await nextTick()

    await wrapper.find('[data-test="pwa-update-cta"]').trigger('click')

    expect(applyUpdate).toHaveBeenCalledTimes(1)
  })

  it('renders success and error messages', async () => {
    const wrapper = mountPrompt()
    installOutcome.value = 'accepted'
    installError.value = 'failed'
    await nextTick()

    expect(wrapper.find('[data-test="pwa-install-success"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="pwa-install-error"]').exists()).toBe(true)
  })
})
