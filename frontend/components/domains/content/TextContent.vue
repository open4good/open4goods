<script setup lang="ts">
import { useContentBloc } from '~/composables/content/useContentBloc'
const props = defineProps<{ blocId: string }>()

const { htmlContent, loading, error, fetchBloc } = useContentBloc()

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
  <div class="text-content">
    <v-progress-circular v-if="loading" indeterminate />
    <v-alert v-else-if="error" type="error" variant="tonal">{{ error }}</v-alert>
    <div v-else v-html="htmlContent" />
  </div>
</template>

<style scoped>
.text-content {
  padding: 1rem 0;
}
</style>
