import type { InjectionKey } from 'vue'
import { inject, provide } from 'vue'

export type GuideContext = {
  verticalId: string | null
  categorySlug: string
  categoryPath: string
  categoryTitle: string
  heroImage: string | null
}

const guideContextKey: InjectionKey<GuideContext> = Symbol('guide-context')

export const provideGuideContext = (context: GuideContext) => {
  provide(guideContextKey, context)
}

export const useGuideContext = () => inject(guideContextKey, null)
