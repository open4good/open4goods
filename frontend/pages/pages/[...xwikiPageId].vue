<script setup lang="ts">
import { unref } from 'vue'
import { useFullPage } from '~/composables/content/useFullPage'
import { useAuth } from '~/composables/useAuth'
import { useRuntimeConfig } from '#app'

const route = useRoute()
const param = computed(() => {
  const p = route.params.xwikiPageId
  return Array.isArray(p) ? p.join('/') : (p as string) || ''
})

const { htmlContent, editLink, loading, error, fetchPage } = useFullPage()
const { isLoggedIn, hasRole } = useAuth()
const config = useRuntimeConfig()

const canEdit = computed(() => {
  const link = unref(editLink)
  const roles = (config.public.editRoles as string[]) || []
  return isLoggedIn.value && !!link && roles.some(role => hasRole(role))
})

onMounted(() => {
  if (param.value) fetchPage(param.value)
})

watch(param, newId => {
  if (newId) fetchPage(newId)
})
</script>

<template>
  <div class="page-content" :class="{ editable: canEdit }">
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{ error }}</v-alert>
    <div v-else v-html="htmlContent" />
    <a
      v-if="canEdit"
      :href="editLink"
      target="_blank"
      rel="noopener"
      class="edit-link"
    >
      Edit
    </a>
  </div>
</template>

<style scoped>
.page-content {
  padding: 1rem 0;
  position: relative;
}
.page-content.editable {
  border: 1px solid #ccc;
}
.edit-link {
  position: absolute;
  bottom: 0.25rem;
  right: 0.25rem;
  font-size: 0.875rem;
}
</style>
