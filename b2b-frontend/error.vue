<template>
  <ErrorPage :error="error" @clear="handleClearError" />
</template>

<script setup lang="ts">
import type { NuxtError } from '#app'
import ErrorPage from '~/components/error/ErrorPage.vue'

const props = defineProps<{
  error: NuxtError
}>()

const handleClearError = () => {
  clearError({ redirect: '/' })
}

useHead({
  title: props.error?.statusCode === 404 ? 'Page Not Found — Infera' : 'System Error — Infera',
  meta: [
    { name: 'robots', content: 'noindex, nofollow' }
  ]
})
</script>
