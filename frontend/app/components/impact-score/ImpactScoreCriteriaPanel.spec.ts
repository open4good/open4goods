import { describe, expect, it, vi } from 'vitest'
import { computed, createSSRApp, defineComponent, h, ref, Suspense } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createVuetify } from 'vuetify'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import ImpactScoreCriteriaPanel from './ImpactScoreCriteriaPanel.vue'

const criteria = ref<Record<string, unknown[]>>({})
const loadAllCriteria = vi.fn(async () => {
  criteria.value = {
    tv: [{ key: 'WARRANTY', name: 'Garantie', utility: 'Durée', icon: null }],
  }
})

mockNuxtImport('useI18n', () => () => ({ t: (key: string) => key }))
vi.mock('~/composables/impact-score/useImpactScoreCriteria', () => ({
  useImpactScoreCriteria: () => ({
    allCriteria: computed(() => Object.values(criteria.value).flat()),
    criteriaByVerticalId: criteria,
    verticalOptions: computed(() => []),
    loadAllCriteria,
    loadCriteriaForVertical: vi.fn(),
  }),
}))

/** GOU-1897 point 4: the SSR HTML used to show "no criteria" because the load only ran in onMounted. */
describe('ImpactScoreCriteriaPanel', () => {
  it('renders the criteria in the server-side HTML', async () => {
    const app = createSSRApp(
      defineComponent({
        render: () => h(Suspense, null, { default: () => h(ImpactScoreCriteriaPanel) }),
      })
    )

    app.use(createVuetify())

    const html = await renderToString(app)

    expect(loadAllCriteria).toHaveBeenCalled()
    expect(html).toContain('Garantie')
    expect(html).not.toContain('impactScoreCriteriaPanel.empty')
  })
})
