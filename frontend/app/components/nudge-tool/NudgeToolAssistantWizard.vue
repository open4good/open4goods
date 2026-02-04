<script setup lang="ts">
import type { NudgeToolConfigDto } from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{
    assistantId?: string | null
    assistantCategoryId?: string | null
    compact?: boolean
  }>(),
  {
    assistantId: null,
    assistantCategoryId: null,
    compact: false,
  }
)

const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const resolvedAssistantId = computed(() => props.assistantId?.trim() ?? '')

const { data: assistantConfig, pending } = await useAsyncData(
  () =>
    resolvedAssistantId.value
      ? `assistant-config-${resolvedAssistantId.value}`
      : 'assistant-config',
  async () => {
    if (!resolvedAssistantId.value) {
      return null
    }

    try {
      return await $fetch<NudgeToolConfigDto>(
        `/api/assistant-configs/${resolvedAssistantId.value}`,
        {
          headers: requestHeaders,
        }
      )
    } catch (error) {
      console.error('Failed to load assistant config:', error)
      return null
    }
  },
  {
    server: true,
    immediate: true,
    watch: [resolvedAssistantId],
  }
)

const resolvedConfig = computed(() => assistantConfig.value ?? null)
</script>

<template>
  <div v-if="resolvedConfig" class="nudge-assistant">
    <NudgeToolWizard
      :assistant-config="resolvedConfig"
      :assistant-category-id="assistantCategoryId"
      :compact="compact"
    />
  </div>
  <v-skeleton-loader v-else-if="pending" type="card" class="nudge-assistant" />
</template>

<style scoped lang="scss">
.nudge-assistant {
  margin-top: 32px;
}
</style>
