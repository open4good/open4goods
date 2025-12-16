<script setup lang="ts">
import { computed, unref, toRef } from 'vue'
import { useContentBloc } from '~/composables/content/useContentBloc'
import { useAuth } from '~/composables/useAuth'
import { useRuntimeConfig } from '#app'
import {
  DEFAULT_LOREM_LENGTH,
  _generateLoremIpsum,
} from '~/utils/content/_loremIpsum'
import '~/assets/css/text-content.css'

// Props

const props = withDefaults(
  defineProps<{
    blocId: string
    defaultLength?: number
    ipsumLength?: number
    fallbackText?: string
  }>(),
  {
    defaultLength: DEFAULT_LOREM_LENGTH,
    ipsumLength: undefined,
    fallbackText: undefined,
  }
)

// Composables
const blocId = toRef(props, 'blocId')
const fallbackText = toRef(props, 'fallbackText')
const { htmlContent, editLink, pending, error } = await useContentBloc(blocId)
const { isLoggedIn, hasRole } = useAuth()
const config = useRuntimeConfig()

// Roles & auth
const canEdit = computed(() => {
  const link = unref(editLink)
  const roles = (config.public.editRoles as string[]) || []
  return isLoggedIn.value && !!link && roles.some(role => hasRole(role))
})

const fallbackLoremLength = computed(
  () => props.ipsumLength ?? props.defaultLength ?? DEFAULT_LOREM_LENGTH
)

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')

const fallbackHtml = computed(() => {
  const raw = fallbackText.value?.trim()

  if (!raw) {
    return null
  }

  return `<p>${escapeHtml(raw)}</p>`
})

const displayHtml = computed(() => {
  const rawContent = (unref(htmlContent) ?? '').trim()
  if (rawContent) {
    return rawContent
  }

  if (fallbackHtml.value) {
    return fallbackHtml.value
  }

  return _generateLoremIpsum(fallbackLoremLength.value)
})
</script>

<template>
  <div class="text-content" :class="{ editable: canEdit }">
    <v-progress-circular v-if="pending" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{
      error
    }}</v-alert>

    <!-- Encapsulated XWiki content -->
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-else class="xwiki-sandbox" v-html="displayHtml" />

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
  display: block;
  font-family: inherit;
}

.xwiki-sandbox * {
  box-sizing: border-box;
  font-family: inherit;
}
</style>
