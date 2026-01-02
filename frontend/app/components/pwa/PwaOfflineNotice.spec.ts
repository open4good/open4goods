import { mount } from '@vue/test-utils'
import { defineComponent, h, ref, nextTick } from 'vue'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

const messages: Record<string, string> = {
  'pwa.offline.title': 'Connection lost',
  'pwa.offline.description': 'Some features may be unavailable.',
  'pwa.offline.indicatorLabel': 'Offline mode',
  'pwa.offline.mobileHelper': 'Content stays cached.',
  'pwa.offline.cta': 'Retry',
  'pwa.offline.dismiss': 'Hide',
}

const onlineStatus = ref(false)
const offlineDismissed = ref(false)
const displayMock = { smAndDown: ref(false) }

vi.mock('@vueuse/core', () => ({
  useOnline: () => onlineStatus,
  useStorage: (_key: string, defaultValue: boolean) => ref(defaultValue),
}))

vi.mock('vuetify', () => ({
  useDisplay: () => displayMock,
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

type OfflineNoticeComponent =
  (typeof import('./PwaOfflineNotice.vue'))['default']
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

const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  props: { modelValue: { type: Boolean, default: false } },
  setup(props, { slots }) {
    return () =>
      h(
        'div',
        { class: 'v-tooltip-stub', 'data-open': String(props.modelValue) },
        [
          slots.activator
            ? h(
                'div',
                { class: 'v-tooltip-stub__activator' },
                slots.activator({ props: {} })
              )
            : null,
          slots.default
            ? h('div', { class: 'v-tooltip-stub__content' }, slots.default())
            : null,
        ]
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
        slots.default ? slots.default() : []
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  setup(_, { slots }) {
    return () =>
      h('span', { class: 'v-icon-stub' }, slots.default ? slots.default() : [])
  },
})

const mountNotice = () =>
  mount(PwaOfflineNotice, {
    global: {
      stubs: {
        VBtn: VBtnStub,
        VTooltip: VTooltipStub,
        VIcon: VIconStub,
      },
      components: {
        ClientOnly: ClientOnlyStub,
      },
    },
  })

describe('PwaOfflineNotice', () => {
  beforeEach(() => {
    resetState()
    displayMock.smAndDown.value = false
    navigatorOnlineSpy?.mockRestore()
    reloadSpy?.mockRestore()
    navigatorOnlineSpy = vi
      .spyOn(window.navigator, 'onLine', 'get')
      .mockReturnValue(false)
    reloadSpy = vi.spyOn(window.location, 'reload').mockImplementation(() => {})
  })

  it('renders the desktop tooltip indicator when offline', async () => {
    const wrapper = mountNotice()
    await nextTick()

    expect(wrapper.find('[data-test="pwa-offline-indicator"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-test="pwa-offline-tooltip"]').text()).toContain(
      'Connection lost'
    )
  })

  it('dismisses the indicator and persists the flag', async () => {
    const wrapper = mountNotice()
    await nextTick()

    await wrapper.find('[data-test="pwa-offline-dismiss"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-test="pwa-offline-indicator"]').exists()).toBe(
      false
    )
    expect(offlineDismissed.value).toBe(true)
  })

  it('renders the compact mobile card when display is small', async () => {
    displayMock.smAndDown.value = true
    const wrapper = mountNotice()
    await nextTick()

    expect(wrapper.find('.pwa-offline-indicator__mobile').exists()).toBe(true)
  })

  it('retries loading when online', async () => {
    const wrapper = mountNotice()
    await nextTick()
    navigatorOnlineSpy.mockReturnValue(true)

    await wrapper.find('[data-test="pwa-offline-retry"]').trigger('click')

    expect(reloadSpy).toHaveBeenCalledTimes(1)
  })
})
