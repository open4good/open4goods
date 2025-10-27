<script setup lang="ts">
import { computed } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import type { WikiPageConfig } from '~~/shared/api-client'
import XwikiFullPage from '../xwiki-fullpage.vue'
import { useCategories } from '~/composables/categories/useCategories'
import {
  deriveWikiPageIdFromUrl,
  registerDynamicWikiRoute,
  unregisterDynamicWikiRoute,
} from '~~/shared/utils/localized-routes'

const route = useRoute()
const { locale } = useI18n()

const rawSlug = route.params.categorySlug
const slug = Array.isArray(rawSlug) ? rawSlug[0] ?? '' : rawSlug ?? ''
const wikiParam = route.params.wikiPage
const wikiSegments = Array.isArray(wikiParam) ? wikiParam : [wikiParam]
const wikiPath = wikiSegments.filter((segment): segment is string => Boolean(segment)).join('/')

const slugPattern = /^[a-z-]+$/

if (!slugPattern.test(slug) || !wikiPath) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const { selectCategoryBySlug } = useCategories()

const { data: categoryDetail } = await useAsyncData(
  `category-wiki-${slug}`,
  async () => {
    try {
      return await selectCategoryBySlug(slug)
    } catch (error) {
      if (error instanceof Error && error.name === 'CategoryNotFoundError') {
        throw createError({ statusCode: 404, statusMessage: error.message, cause: error })
      }

      throw error
    }
  },
  { server: true, immediate: true },
)

const category = computed(() => categoryDetail.value ?? null)

const normalizedWikiPath = computed(() => wikiPath.replace(/^\/+|\/+$/gu, ''))

const wikiPageEntry = computed<WikiPageConfig | null>(() => {
  const pages = category.value?.wikiPages ?? []
  const targetPath = normalizedWikiPath.value

  if (!targetPath) {
    return null
  }

  return (
    pages.find((page) => {
      const candidate = (page.verticalUrl ?? '').replace(/^\/+|\/+$/gu, '')
      return candidate === targetPath
    }) ?? null
  )
})

const wikiPageId = computed(() => {
  const entry = wikiPageEntry.value

  if (!entry) {
    return null
  }

  return deriveWikiPageIdFromUrl(entry.wikiUrl ?? null)
})

const pageId = wikiPageId.value

if (!wikiPageEntry.value || !pageId) {
  throw createError({ statusCode: 404, statusMessage: 'Wiki page not found' })
}

registerDynamicWikiRoute(route.path, {
  pageId,
  locale: locale.value,
  name: `category-wiki:${slug}:${normalizedWikiPath.value}`,
})

onBeforeRouteLeave(() => {
  unregisterDynamicWikiRoute(route.path)
})
</script>

<template>
  <component :is="XwikiFullPage" />
</template>
