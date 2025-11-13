import { mount } from '@vue/test-utils'
import { defineComponent, h, ref, nextTick } from 'vue'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

const messages: Record<string, string> = {
  'pwa.offline.title': 'Connection lost',
  'pwa.offline.description': 'Some features may be unavailable.',
  'pwa.offline.cta': 'Retry',
  'pwa.offline.dismiss': 'Hide',
}

const onlineStatus = ref(false)
const offlineDismissed = ref(false)

vi.mock('@vueuse/core', () => ({
  useOnline: () => onlineStatus,
}))

let navigatorOnlineSpy: ReturnType<typeof vi.spyOn>
let reloadSpy: ReturnType<typeof vi.spyOn>

vi.mock('~~/app/composables/pwa/usePwaOfflineNoticeBridge', () => ({
  usePwaOfflineNoticeBridge: () => ({
    t: (key: string) => messages[key] ?? key,
    isOnline: onlineStatus,
    offlineDismissed,
  }),
}))

type OfflineNoticeComponent = typeof import('./PwaOfflineNotice.vue')['default']
let PwaOfflineNotice: OfflineNoticeComponent

beforeAll(async () => {
  PwaOfflineNotice = (await import('./PwaOfflineNotice.vue')).default
})

const resetState = () => {
  onlineStatus.value = false
  offlineDismissed.value = false
}

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

const mountNotice = () =>
  mount(PwaOfflineNotice, {
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

describe('PwaOfflineNotice', () => {
  beforeEach(() => {
    resetState()
    navigatorOnlineSpy?.mockRestore()
    reloadSpy?.mockRestore()
    navigatorOnlineSpy = vi.spyOn(window.navigator, 'onLine', 'get').mockReturnValue(false)
    reloadSpy = vi.spyOn(window.location, 'reload').mockImplementation(() => {})
  })

  it('renders a banner when offline and not dismissed', async () => {
    const wrapper = mountNotice()
    await nextTick()

    expect(wrapper.find('[data-test="pwa-offline-banner"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Connection lost')
  })

  it('dismisses the banner and persists the flag', async () => {
    const wrapper = mountNotice()
    await nextTick()

    await wrapper.find('[data-test="pwa-offline-dismiss"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-test="pwa-offline-banner"]').exists()).toBe(false)
    expect(offlineDismissed.value).toBe(true)
  })

  it('retries loading when online', async () => {
    const wrapper = mountNotice()
    await nextTick()
    navigatorOnlineSpy.mockReturnValue(true)

    await wrapper.find('[data-test="pwa-offline-retry"]').trigger('click')

    expect(reloadSpy).toHaveBeenCalledTimes(1)
  })
})
