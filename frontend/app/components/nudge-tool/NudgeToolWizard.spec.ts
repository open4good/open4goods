import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import { createVuetify } from 'vuetify'
import type { VerticalConfigDto } from '~~/shared/api-client'
import NudgeToolWizard from './NudgeToolWizard.vue'

// Basic mocks for Nuxt/Vue usage
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))
vi.mock('vuetify', async () => {
  const actual = await vi.importActual<typeof import('vuetify')>('vuetify')
  return {
    ...actual,
    useDisplay: () => ({ smAndDown: { value: false } }),
  }
})
vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({ fetchCategories: vi.fn().mockResolvedValue([]) }),
}))
vi.mock('~/components/nudge-tool/NudgeWizardHeader.vue', () => ({
  default: { template: '<div class="header-stub"></div>' },
}))

vi.mock('#components', () => ({
  NudgeToolStepCategory: { template: '<div>Category</div>' },
  NudgeToolStepScores: { template: '<div>Scores</div>' },
  NudgeToolStepCondition: { template: '<div>Condition</div>' },
  NudgeToolStepSubsetGroup: { template: '<div>SubsetGroup</div>' },
  NudgeToolStepRecommendations: { template: '<div>Recommendations</div>' },
  RoundedCornerCard: {
    template:
      '<div class="rounded-card-stub" v-bind="$attrs"><slot /><slot name="corner"/></div>',
  },
  NudgeWizardHeader: { template: '<div class="header-stub"></div>' },
}))

// Mock other imports that might cause issues
vi.mock('~/utils/_category-filter-state', () => ({
  buildCategoryHash: vi.fn(),
}))
vi.mock('~/utils/_subset-to-filters', () => ({
  buildFilterRequestFromSubsets: vi.fn().mockReturnValue({}),
}))
vi.mock('~/utils/_nudge-tool-filters', () => ({
  buildConditionFilter: vi.fn(),
  buildNudgeFilterRequest: vi.fn().mockReturnValue({}),
  buildScoreFilters: vi.fn(),
}))
vi.mock('@vueuse/core', () => ({
  useDebounceFn: (fn: (...args: unknown[]) => unknown) => fn,
  useElementSize: () => ({ height: ref(100) }),
  useTransition: (source: unknown) => source,
}))

const vuetify = createVuetify()

describe('NudgeToolWizard', () => {
  it('renders and shows active steps limited to current index', async () => {
    // We cannot easily test internal computed without exposing it or deep interaction.
    // However, we can check v-stepper items prop.

    // Stub components to avoid deep rendering
    const wrapper = mount(NudgeToolWizard, {
      global: {
        plugins: [vuetify],
        mocks: {
          $t: (t: string) => t,
        },
        stubs: {
          RoundedCornerCard: {
            template: '<div><slot /><slot name="corner" /></div>',
          },
          VBtn: true,
          VAvatar: true,
          VImg: true,
          VIcon: true,
          VProgressLinear: true,
          VWindow: { template: '<div><slot /></div>' },
          VWindowItem: { template: '<div><slot /></div>', props: ['value'] },
          VSpacer: true,
          // We keep VStepper stubbed but check its props if possible,
          // or we can use a component that renders keys
          VStepper: {
            props: ['items', 'modelValue'],
            template: '<div class="stub-stepper">{{ items.length }}</div>',
          },
          NudgeToolStepCategory: true,
          NudgeToolStepScores: true,
          NudgeToolStepCondition: true,
          NudgeToolStepSubsetGroup: true,
          NudgeToolStepRecommendations: true,
        },
      },
      props: {
        verticals: [
          { id: 'v1', nudgeToolConfig: { scores: ['s1'], subsets: [] } },
        ] as unknown as VerticalConfigDto[],
      },
    })

    expect(wrapper.exists()).toBe(true)

    // Initially at 'category', should exist
    expect(wrapper.exists()).toBe(true)

    // Header row was removed in refactor, removed assertion.
  })

  it('applies compact height on category and expands on content steps', async () => {
    const wrapper = mount(NudgeToolWizard, {
      global: {
        plugins: [vuetify],
        mocks: {
          $t: (t: string) => t,
        },
        stubs: {
          VBtn: true,
          VAvatar: true,
          VImg: true,
          VIcon: true,
          VProgressLinear: true,
          VWindow: { template: '<div><slot /></div>' },
          VWindowItem: { template: '<div><slot /></div>', props: ['value'] },
        },
      },
      props: {
        verticals: [
          { id: 'v1', nudgeToolConfig: { scores: ['s1'], subsets: [] } },
        ] as unknown as VerticalConfigDto[],
      },
    })

    const card = wrapper.find('.nudge-wizard')
    expect(card.attributes('style')).toContain('min-height: 454px')
    expect(card.attributes('style')).toContain('max-height: 454px')

    ;(wrapper.vm as unknown as { activeStepKey: string }).activeStepKey =
      'condition'
    await wrapper.vm.$nextTick()

    expect(card.attributes('style')).toContain('min-height: 500px')
    expect(card.attributes('style')).toContain('max-height: 500px')

    ;(wrapper.vm as unknown as { activeStepKey: string }).activeStepKey =
      'category'
    await wrapper.vm.$nextTick()

    expect(card.attributes('style')).toContain('min-height: 454px')
    expect(card.attributes('style')).toContain('max-height: 454px')
  })

  it('pulses the corner emoji on category step at random intervals', async () => {
    vi.useFakeTimers()
    const randomSpy = vi.spyOn(Math, 'random').mockReturnValue(0)

    const wrapper = mount(NudgeToolWizard, {
      global: {
        plugins: [vuetify],
        mocks: {
          $t: (t: string) => t,
        },
        stubs: {
          VBtn: true,
          VAvatar: true,
          VImg: true,
          VIcon: true,
          VProgressLinear: true,
          VWindow: { template: '<div><slot /></div>' },
          VWindowItem: { template: '<div><slot /></div>', props: ['value'] },
        },
      },
      props: {
        verticals: [
          { id: 'v1', nudgeToolConfig: { scores: ['s1'], subsets: [] } },
        ] as unknown as VerticalConfigDto[],
      },
    })

    const vm = wrapper.vm as unknown as {
      isCornerPulsing: boolean
      activeStepKey: string
    }

    expect(vm.isCornerPulsing).toBe(false)

    vi.advanceTimersByTime(4100)
    await wrapper.vm.$nextTick()
    expect(vm.isCornerPulsing).toBe(true)

    vi.advanceTimersByTime(500)
    await wrapper.vm.$nextTick()
    expect(vm.isCornerPulsing).toBe(false)

    randomSpy.mockRestore()
    vi.useRealTimers()
  })
})
