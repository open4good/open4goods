<template>
  <nav v-if="meta" class="flex justify-center mt-4 items-center gap-2">
    <button
      class="px-3 py-1 border rounded disabled:opacity-50"
      :disabled="meta.number === 0"
      @click="change(meta.number - 1)"
    >
      {{ t('blog.previous') }}
    </button>
    <span class="px-2">{{ meta.number + 1 }} / {{ meta.totalPages }}</span>
    <button
      class="px-3 py-1 border rounded disabled:opacity-50"
      :disabled="meta.number + 1 >= meta.totalPages"
      @click="change(meta.number + 1)"
    >
      {{ t('blog.next') }}
    </button>
  </nav>
</template>

<script setup lang="ts">
import { useI18n } from '#imports'
import type { PageMetaDto } from '@/api'

defineProps<{ meta: PageMetaDto }>()
const emit = defineEmits<{
  (e: 'change', page: number): void
}>()

const { t } = useI18n()

function change(page: number) {
  emit('change', page)
}
</script>
