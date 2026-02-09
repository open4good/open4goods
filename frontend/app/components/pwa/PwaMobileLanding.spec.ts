import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const messages: Record<string, string> = {
  'home.hero.title': 'Nudger home',
  'pwa.landing.hero.subtitle': 'Mobile subtitle',
  'pwa.landing.search.title': 'Search',
  'pwa.landing.search.label': 'Search label',
  'pwa.landing.search.placeholder': 'Search placeholder',
  'pwa.landing.search.ariaLabel': 'Search aria',
  'pwa.landing.search.cta': 'Search now',
  'pwa.landing.actions.scan.title': 'Scan',
  'pwa.landing.actions.scan.description': 'Scan description',
  'pwa.landing.actions.scan.helper': 'Scan helper',
  'pwa.landing.actions.scan.loading': 'Loading',
  'pwa.landing.actions.scan.error': 'Error',
  'pwa.landing.actions.wizard.title': 'Assistant Nudger',
  'pwa.landing.actions.wizard.description': 'Wizard description',
  'pwa.landing.actions.search.title': 'Search action',
  'pwa.landing.actions.search.description': 'Search action description',
}

const smAndDown = { value: true }

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => messages[key] ?? key,
  }),
}))

vi.mock('vuetify', () => ({
  useDisplay: () => ({
    smAndDown,
  }),
}))

describe('PwaMobileLanding', () => {
  beforeEach(() => {
    smAndDown.value = true
  })

  it('opens the wizard dialog from the hero button', async () => {
    const component = (await import('./PwaMobileLanding.vue')).default

    const wrapper = mount(component, {
      global: {
        stubs: {
          VContainer: { template: '<div><slot /></div>' },
          VRow: { template: '<div><slot /></div>' },
          VCol: { template: '<div><slot /></div>' },
          VSheet: { template: '<div><slot /></div>' },
          VCard: { template: '<div><slot /></div>' },
          VCardTitle: { template: '<div><slot /></div>' },
          VCardText: { template: '<div><slot /></div>' },
          VCardItem: { template: '<div><slot /></div>' },
          VCardSubtitle: { template: '<div><slot /></div>' },
          VAvatar: { template: '<div><slot /></div>' },
          VIcon: { template: '<i><slot /></i>' },
          VChip: { template: '<div><slot /></div>' },
          ClientOnly: { template: '<div><slot /></div>' },
          VSnackbar: { template: '<div><slot /></div>' },
          SearchSuggestField: { template: '<div />' },
          CategoryBadgesRow: { template: '<div />' },
          PwaBarcodeScanner: { template: '<div />' },
          NudgeToolWizard: { template: '<div data-testid="nudge-tool-wizard" />' },
          VBtn: {
            emits: ['click'],
            template:
              '<button @click="$emit(\'click\')"><slot /></button>',
          },
          VDialog: {
            props: ['modelValue'],
            template:
              '<div v-if="modelValue" class="v-dialog-stub"><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-testid="nudge-tool-wizard"]').exists()).toBe(
      false
    )

    await wrapper.get('[data-testid="pwa-open-wizard"]').trigger('click')

    expect(wrapper.find('[data-testid="nudge-tool-wizard"]').exists()).toBe(
      true
    )
  })

  it('opens the wizard dialog from the wizard quick action card', async () => {
    const component = (await import('./PwaMobileLanding.vue')).default

    const wrapper = mount(component, {
      global: {
        stubs: {
          VContainer: { template: '<div><slot /></div>' },
          VRow: { template: '<div><slot /></div>' },
          VCol: { template: '<div><slot /></div>' },
          VSheet: { template: '<div><slot /></div>' },
          VCard: {
            emits: ['click'],
            inheritAttrs: false,
            template: '<div v-bind="$attrs" @click="$emit(\'click\')"><slot /></div>',
          },
          VCardTitle: { template: '<div><slot /></div>' },
          VCardText: { template: '<div><slot /></div>' },
          VCardItem: { template: '<div><slot /></div>' },
          VCardSubtitle: { template: '<div><slot /></div>' },
          VAvatar: { template: '<div><slot /></div>' },
          VIcon: { template: '<i><slot /></i>' },
          VChip: { template: '<div><slot /></div>' },
          ClientOnly: { template: '<div><slot /></div>' },
          VSnackbar: { template: '<div><slot /></div>' },
          SearchSuggestField: { template: '<div />' },
          CategoryBadgesRow: { template: '<div />' },
          PwaBarcodeScanner: { template: '<div />' },
          NudgeToolWizard: { template: '<div data-testid="nudge-tool-wizard" />' },
          VBtn: {
            emits: ['click'],
            template:
              '<button @click="$emit(\'click\')"><slot /></button>',
          },
          VDialog: {
            props: ['modelValue'],
            template:
              '<div v-if="modelValue" class="v-dialog-stub"><slot /></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-testid="nudge-tool-wizard"]').exists()).toBe(
      false
    )

    await wrapper.get('[data-testid="pwa-quick-action-wizard"]').trigger('click')

    expect(wrapper.find('[data-testid="nudge-tool-wizard"]').exists()).toBe(
      true
    )
  })
})
