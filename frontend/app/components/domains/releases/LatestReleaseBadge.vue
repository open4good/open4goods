<template>
  <v-chip
    v-if="latestName"
    color="primary"
    prepend-icon="mdi-rocket-launch"
    class="latest-release-badge"
    variant="flat"
  >
    {{ t('releases.latestLabel', { name: latestName }) }}
  </v-chip>
</template>

<script setup lang="ts">
const { t } = useI18n()

const { data: latestRelease } = await useLatestRelease()

const latestName = computed(() => latestRelease.value?.name ?? '')
</script>

<style scoped lang="sass">
.latest-release-badge
  font-weight: 700
  letter-spacing: 0.01em
</style>
