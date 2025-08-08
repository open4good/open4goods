<script setup lang="ts">
import { computed, unref, watch } from 'vue'
import { useWikiPage } from '~/composables/wiki/useWikiPage'
import { useAuth } from '~/composables/useAuth'
import { useRuntimeConfig } from '#app'
import '~/assets/css/text-content.css'

const route = useRoute()
const slugParam = route.params.slug as string[] | string
const slug = Array.isArray(slugParam) ? slugParam.join('.') : slugParam

const { data, loading, error, fetchPage } = useWikiPage()
const { isLoggedIn, hasRole } = useAuth()
const config = useRuntimeConfig()

await fetchPage(slug)

watch(
  () => route.params.slug,
  newSlug => {
    const s = Array.isArray(newSlug) ? newSlug.join('.') : (newSlug as string)
    fetchPage(s)
  }
)

const canEdit = computed(() => {
  const link = unref(data.value?.editLink)
  const roles = (config.public.editRoles as string[]) || []
  return isLoggedIn.value && !!link && roles.some(role => hasRole(role))
})

useHead(() => ({
  title: data.value?.metaTitle,
  meta: [
    { name: 'description', content: data.value?.metaDescription }
  ]
}))
</script>

<template>
  <div class="wiki-page" :class="{ editable: canEdit }">
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{ error }}</v-alert>

    <div v-else class="xwiki-sandbox" v-html="data?.html" />

    <a
      v-if="canEdit"
      :href="data?.editLink"
      target="_blank"
      rel="noopener"
      class="edit-link"
    >
      Edit
    </a>
  </div>
</template>

<style scoped>
.wiki-page {
  position: relative;
  padding: 1rem 0;
}

.wiki-page.editable {
  border: 1px solid #ccc;
}

.edit-link {
  position: absolute;
  bottom: 0.25rem;
  right: 0.25rem;
  font-size: 0.875rem;
}
</style>
