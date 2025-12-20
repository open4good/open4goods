<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { matchLocalizedWikiRouteByPath } from '~~/shared/utils/localized-routes'

const route = useRoute()

definePageMeta({ lazy: true })

const XwikiFullPageRenderer = defineAsyncComponent(
  () => import('~/components/cms/XwikiFullPageRenderer.vue')
)

const matchedRoute = computed(() => matchLocalizedWikiRouteByPath(route.path))

if (!matchedRoute.value) {
  throw createError({ statusCode: 404, statusMessage: 'CMS page not found' })
}

const pageId = computed(() => matchedRoute.value?.pageId ?? null)
</script>

<template>
  <XwikiFullPageRenderer :page-id="pageId" />
</template>
