<script setup lang="ts">
import BuyingGuideRenderer from '~/components/docs/BuyingGuideRenderer.vue'
import {
  resolveGuideDocPath,
  useDocsContent,
  type DocsDoc,
} from '~/composables/useDocsContent'

definePageMeta({ lazy: true })

const route = useRoute()
const requestURL = useRequestURL()
const { getDocByPath } = useDocsContent()

const rawSlug = route.params.slug
const slugSegments = Array.isArray(rawSlug)
  ? rawSlug.filter((s): s is string => typeof s === 'string')
  : typeof rawSlug === 'string'
    ? [rawSlug]
    : []

const guideSlug = slugSegments.join('/')

if (!guideSlug) {
  throw createError({
    statusCode: 404,
    message: 'Guide not found',
    fatal: false,
  })
}

const doc = (await getDocByPath({
  path: resolveGuideDocPath({ verticalId: 'default', guideSlug }),
})) as DocsDoc | null

if (!doc) {
  throw createError({
    statusCode: 404,
    message: 'Guide not found',
    fatal: false,
  })
}

const canonicalPath = `/guides/${guideSlug}`
const canonicalUrl = new URL(canonicalPath, requestURL.origin).toString()

useSeoMeta({
  title: () => doc.title ?? guideSlug,
  description: () => doc.description ?? '',
  ogTitle: () => doc.title ?? guideSlug,
  ogDescription: () => doc.description ?? '',
  ogUrl: () => canonicalUrl,
})

useHead({
  link: [{ rel: 'canonical', href: canonicalUrl }],
})

const { t } = useI18n()

const breadcrumbs = [{ title: t('buyingGuide.breadcrumbs.home'), to: '/' }]

const guideContext = {
  verticalId: 'default',
  categorySlug: 'guides',
  categoryPath: '/guides',
  categoryTitle: '',
  heroImage: null,
}
</script>

<template>
  <BuyingGuideRenderer
    :doc="doc"
    :guide-context="guideContext"
    :breadcrumbs="breadcrumbs"
  />
</template>
