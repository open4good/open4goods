<script setup lang="ts">
const props = withDefaults(defineProps<{ scrollTarget?: string }>(), {
  scrollTarget: undefined,
})

const { t } = useI18n()

const { data: latestRelease } = await useLatestRelease()

const latestName = computed(() => latestRelease.value?.name ?? '')

const handleClick = (event: MouseEvent) => {
  const { scrollTarget } = props

  if (!import.meta.client || !scrollTarget || !scrollTarget.startsWith('#')) {
    return
  }

  event.preventDefault()
  const target = document.querySelector(scrollTarget)
  if (target instanceof HTMLElement) {
    target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
</script>

<template>
  <v-chip
    v-if="latestName"
    class="latest-release-badge"
    color="primary"
    prepend-icon="mdi-rocket-launch"
    :href="props.scrollTarget"
    :ripple="Boolean(props.scrollTarget)"
    :role="props.scrollTarget ? 'link' : undefined"
    variant="flat"
    @click="handleClick"
  >
    {{ t('releases.latestLabel', { name: latestName }) }}
  </v-chip>
</template>

<style scoped lang="sass">
.latest-release-badge
  font-weight: 700
  letter-spacing: 0.01em
</style>
