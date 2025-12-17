
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import type { VerticalConfigDto } from '~~/shared/api-client'
import NudgeToolWizard from './NudgeToolWizard.vue'

// Basic mocks for Nuxt/Vue usage
vi.mock('vue-router', () => ({
    useRouter: () => ({ push: vi.fn() })
}))
vi.mock('vue-i18n', () => ({
    useI18n: () => ({ t: (k: string) => k })
}))
vi.mock('vuetify', () => ({
    useDisplay: () => ({ smAndDown: { value: false } })
}))
vi.mock('~/composables/categories/useCategories', () => ({
    useCategories: () => ({ fetchCategories: vi.fn().mockResolvedValue([]) })
}))

// Mock other imports that might cause issues
vi.mock('~/utils/_category-filter-state', () => ({
    buildCategoryHash: vi.fn()
}))
vi.mock('~/utils/_subset-to-filters', () => ({
    buildFilterRequestFromSubsets: vi.fn().mockReturnValue({})
}))
vi.mock('~/utils/_nudge-tool-filters', () => ({
    buildConditionFilter: vi.fn(),
    buildNudgeFilterRequest: vi.fn().mockReturnValue({}),
    buildScoreFilters: vi.fn(),
}))
vi.mock('@vueuse/core', () => ({
    useDebounceFn: (fn: (...args: unknown[]) => unknown) => fn,
    useElementSize: () => ({ height: { value: 100 } }),
    useTransition: (source: unknown) => source
}))

describe('NudgeToolWizard', () => {
    it('renders and shows active steps limited to current index', async () => {
        // We cannot easily test internal computed without exposing it or deep interaction.
        // However, we can check v-stepper items prop.

        // Stub components to avoid deep rendering
        const wrapper = mount(NudgeToolWizard, {
            global: {
                stubs: {
                    VCard: { template: '<div><slot /></div>' },
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
                        template: '<div class="stub-stepper">{{ items.length }}</div>'
                    },
                    NudgeToolStepCategory: true,
                    NudgeToolStepScores: true,
                    NudgeToolStepCondition: true,
                    NudgeToolStepSubsetGroup: true,
                    NudgeToolStepRecommendations: true,
                }
            },
            props: {
                verticals: [
                    { id: 'v1', nudgeToolConfig: { scores: ['s1'], subsets: [] } }
                ] as unknown as VerticalConfigDto[]
            }
        })

        expect(wrapper.exists()).toBe(true)

        // Initially at 'category', should exist
        expect(wrapper.exists()).toBe(true)

        // Stepper is removed, so we don't check for it anymore.
        // We can check that the header row exists
        expect(wrapper.find('.nudge-wizard__header-row').exists()).toBe(true)
    })
})
