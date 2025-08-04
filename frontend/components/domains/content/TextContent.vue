<script setup lang="ts">
import { unref } from 'vue'
import { useContentBloc } from '~/composables/content/useContentBloc'
import { useAuth } from '~/composables/useAuth'
import { useRuntimeConfig } from '#app'

const props = defineProps<{ blocId: string }>()

// Retrieve bloc content along with its optional edit link
const { htmlContent, editLink, loading, error, fetchBloc } = useContentBloc()

// Access authentication state and role checker
const { isLoggedIn, hasRole } = useAuth()

// Runtime configuration contains the roles allowed to edit content
const config = useRuntimeConfig()

// Determine if the current user can edit the bloc content
const canEdit = computed(() => {
  const link = unref(editLink)
  const roles = (config.public.editRoles as string[]) || []
  return isLoggedIn.value && !!link && roles.some(role => hasRole(role))
})

// Fetch the content bloc when the component mounts or when blocId changes
onMounted(() => {
  fetchBloc(props.blocId)
})

watch(
  () => props.blocId,
  newId => {
    if (newId) fetchBloc(newId)
  }
)
</script>

<template>
  <!-- Optional border and edit link are displayed only for authorized users -->
  <div class="text-content" :class="{ editable: canEdit }">
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{ error }}</v-alert>
    <div v-else v-html="htmlContent" />
    <!-- Edit link opens the bloc in a new tab when user can edit -->
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
.text-content {
  padding: 1rem 0;
  position: relative; /* Allow absolute positioning for the edit link */
}

.text-content.editable {
  border: 1px solid #ccc; /* Light border indicates editable content */
}

.edit-link {
  position: absolute;
  bottom: 0.25rem;
  right: 0.25rem;
  font-size: 0.875rem;
}
</style>
