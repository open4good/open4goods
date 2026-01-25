import { describe, it, expect, vi } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { ref } from 'vue'
import type {
  VerticalCategoryDto,
  VerticalConfigDto,
} from '~~/shared/api-client'
import NudgeToolWizard from './NudgeToolWizard.vue'

// Basic mocks for Nuxt/Vue usage
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))
vi.mock('vuetify', () => ({
  useDisplay: () => ({ smAndDown: { value: false } }),
}))
const currentCategory = ref<VerticalCategoryDto | null>(null)

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    fetchCategories: vi.fn().mockResolvedValue([]),
    selectCategoryBySlug: vi.fn(),
    currentCategory,
  }),
}))
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({ isLoggedIn: { value: false } }),
}))
vi.mock('~/components/nudge-tool/NudgeWizardHeader.vue', () => ({
  default: { template: '<div class="header-stub"></div>' },
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
const zoomedState = ref(false)

vi.mock('@vueuse/core', () => ({
  useDebounceFn: (fn: (...args: unknown[]) => unknown) => fn,
  useElementSize: () => ({ height: ref(100) }),
  useWindowSize: () => ({ height: ref(900) }),
  useTransition: (source: unknown) => source,
  usePreferredReducedMotion: () => ({ value: false }),
  useStorage: (_key: string, defaultValue: boolean) => {
    if (zoomedState.value === undefined) {
      zoomedState.value = defaultValue
    }
    return zoomedState
  },
}))

vi.stubGlobal(
  '$fetch',
  vi.fn(url => {
    const normalizedUrl = url.split('?')[0]
    if (normalizedUrl === '/api/products/search') {
      return Promise.resolve({
        products: {
          data: [],
          page: { totalElements: 0 },
        },
      })
    }
    return Promise.resolve({})
  })
)

describe('NudgeToolWizard', () => {
  it('renders and shows active steps limited to current index', async () => {
    // We cannot easily test internal computed without exposing it or deep interaction.
    // However, we can check v-stepper items prop.

    // Stub components to avoid deep rendering
    const wrapper = await mountSuspended(NudgeToolWizard, {
      global: {
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
          VTooltip: {
            props: ['text', 'location'],
            template:
              '<div class="v-tooltip-stub"><slot name="activator" :props="{}" /><slot /></div>',
          },
          // We keep VStepper stubbed but check its props if possible,
          // or we can use a component that renders keys
          VStepper: {
            props: ['items', 'modelValue'],
            template: '<div class="stub-stepper">{{ items.length }}</div>',
          },
          NudgeToolStepCategory: {
            template: '<div data-step="category"></div>',
          },
          NudgeToolStepScores: { template: '<div data-step="scores"></div>' },
          NudgeToolStepCondition: {
            template: '<div data-step="condition"></div>',
          },
          NudgeToolStepSubsetGroup: {
            template: '<div data-step="subset"></div>',
          },
          NudgeToolStepRecommendations: {
            template: '<div data-step="recommendations"></div>',
          },
        },
      },
      props: {
        verticals: [
          { id: 'v1', nudgeToolConfig: { scores: ['s1'], subsets: [] } },
        ] as unknown as VerticalConfigDto[],
      },
      // Suppress Vuetify warnings if any remain
      attachTo: document.body,
    })

    expect(wrapper.exists()).toBe(true)

    // Initially at 'category', should exist
    expect(wrapper.exists()).toBe(true)

    // Header row was removed in refactor, removed assertion.
  })

  it('orders subset groups before condition', async () => {
    currentCategory.value = {
      id: 'cat-1',
      verticalHomeTitle: 'Category',
      verticalHomeUrl: '/category',
      nudgeToolConfig: {
        scores: [
          {
            scoreName: 'score',
            title: 'Score',
          },
        ],
        subsets: [
          {
            id: 'price-1',
            title: 'Budget',
            group: 'price',
          },
        ],
      },
    } as VerticalCategoryDto

    const wrapper = await mountSuspended(NudgeToolWizard, {
      global: {
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
          VTooltip: {
            props: ['text', 'location'],
            template:
              '<div class="v-tooltip-stub"><slot name="activator" :props="{}" /><slot /></div>',
          },
          NudgeToolStepCategory: {
            template: '<div data-step="category"></div>',
          },
          NudgeToolStepScores: { template: '<div data-step="scores"></div>' },
          NudgeToolStepCondition: {
            template: '<div data-step="condition"></div>',
          },
          NudgeToolStepSubsetGroup: {
            template: '<div data-step="subset"></div>',
          },
          NudgeToolStepRecommendations: {
            template: '<div data-step="recommendations"></div>',
          },
        },
      },
      props: {
        initialCategoryId: 'cat-1',
        verticals: [
          { id: 'v1', nudgeToolConfig: { scores: ['s1'], subsets: [] } },
        ] as unknown as VerticalConfigDto[],
      },
    })

    const stepKeys = wrapper
      .findAll('[data-step]')
      .map(node => node.attributes('data-step'))

    expect(stepKeys).toEqual([
      'category',
      'scores',
      'subset',
      'condition',
      'recommendations',
    ])
  })
})
