<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{ scrollTarget?: string; dense?: boolean }>(),
  {
    scrollTarget: undefined,
    dense: false,
  }
)

const { t } = useI18n()

const { data: latestRelease } = await useLatestRelease()

const latestName = computed(() => latestRelease.value?.name ?? '')
const badgeAriaLabel = computed(() =>
  t('releases.latestLabel', { name: latestName.value })
)

const handleClick = (event: Event) => {
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
  <a
    v-if="latestName"
    :href="props.scrollTarget"
    class="latest-release-badge"
    :class="{ 'latest-release-badge--dense': props.dense }"
    :role="props.scrollTarget ? 'link' : undefined"
    :aria-label="badgeAriaLabel"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
  >
    <span class="latest-release-badge__value">{{ latestName }}</span>
  </a>
</template>

<style scoped lang="sass">
.latest-release-badge
  display: inline-flex
  align-items: stretch
  gap: 10px
  padding: 6px 6px 6px 6px
  border-radius: 999px
  border: 1px solid rgba(var(--v-theme-on-surface), 0.16)
  color: rgb(var(--v-theme-on-surface))
  text-decoration: none
  box-shadow: 0 6px 20px rgba(var(--v-theme-shadow-primary-600), 0.12)
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease
  width: fit-content

  &:hover
    border-color: rgba(var(--v-theme-primary), 0.4)
    box-shadow: 0 10px 26px rgba(var(--v-theme-shadow-primary-600), 0.18)
    transform: translateY(-1px)

  &--unlocked
    --tilt-angle: 0deg

  &:hover
    border-color: rgba(var(--v-theme-primary), 0.4)
    box-shadow: 0 10px 26px rgba(var(--v-theme-shadow-primary-600), 0.18)
    transform: translateY(-1px) rotate(var(--tilt-angle))

  &:active
    transform: translateY(0)

  &--dense
    padding-block: 4px
    gap: 8px

  &__label
    display: inline-flex
    align-items: center
    gap: 6px
    border-radius: 999px
    background: rgba(var(--v-theme-primary), 0.14)
    color: rgb(var(--v-theme-primary))
    font-weight: 800
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.75rem

  &__value
    display: inline-flex
    align-items: center
    font-weight: 700
    letter-spacing: 0.01em
    padding-right: 6px
    padding-left: 6px
</style>
