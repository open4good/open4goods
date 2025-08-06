<script setup lang="ts">
import { onMounted, watch, computed, unref } from 'vue'
import { useContentBloc } from '~/composables/content/useContentBloc'
import { useAuth } from '~/composables/useAuth'
import { useRuntimeConfig } from '#app'

// Props
const props = defineProps<{ blocId: string }>()

// Composables
const { htmlContent, editLink, loading, error, fetchBloc } = useContentBloc()
const { isLoggedIn, hasRole } = useAuth()
const config = useRuntimeConfig()

// Roles & auth
const canEdit = computed(() => {
  const link = unref(editLink)
  const roles = (config.public.editRoles as string[]) || []
  return isLoggedIn.value && !!link && roles.some(role => hasRole(role))
})

// Watcher / mount
onMounted(() => {
  fetchBloc(props.blocId)
})

watch(
  () => props.blocId,
  newId => {
    if (newId) fetchBloc(newId)
  }
)

// --- Style Injection ---
const BOOTSTRAP_HREF = 'https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css'
const XWIKI_HREF = 'https://wiki.nudger.fr/bin/skin/skins/flamingo/style.min.css'

function injectStylesOnce(href: string) {
  if (document.querySelector(`link[href="${href}"]`)) return
  const link = document.createElement('link')
  link.rel = 'stylesheet'
  link.href = href
  document.head.appendChild(link)
}

onMounted(() => {
  injectStylesOnce(BOOTSTRAP_HREF)
  injectStylesOnce(XWIKI_HREF)
})
</script>

<template>
  <div class="text-content" :class="{ editable: canEdit }">
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{ error }}</v-alert>

    <!-- Encapsulated XWiki content -->
    <div v-else class="xwiki-sandbox" v-html="htmlContent" />

    <!-- Edit link -->
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
  position: relative;
}

.text-content.editable {
  border: 1px solid #ccc;
}

.edit-link {
  position: absolute;
  bottom: 0.25rem;
  right: 0.25rem;
  font-size: 0.875rem;
}

/* Scoped sandbox to contain Bootstrap + XWiki styles */
.xwiki-sandbox {
  all: initial;
  display: block;
  font-family: inherit;
}

.xwiki-sandbox * {
  all: unset;
  display: revert;
  font-family: inherit;
  box-sizing: border-box;
}
</style>
