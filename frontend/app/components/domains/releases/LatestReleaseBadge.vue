<script setup lang="ts">
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
const badgeLabel = computed(() => t('releases.latest'))
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
    data-testid="latest-release-badge"
    :role="props.scrollTarget ? 'link' : undefined"
    :aria-label="badgeAriaLabel"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
  >
    <span class="latest-release-badge__pill">{{ badgeLabel }}</span>
    <span class="latest-release-badge__value">{{ latestName }}</span>
  </a>
</template>

<style scoped lang="sass">
.latest-release-badge
  display: inline-flex
  align-items: center
  gap: 10px
  padding: 6px 12px
  border-radius: 12px
  border: 1px solid #d0d7de
  background: linear-gradient(180deg, #f6f8fa, #eaeef2)
  color: #24292f
  text-decoration: none
  box-shadow: inset 0 1px 0 #ffffff, 0 1px 0 rgba(0, 0, 0, 0.04)
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease
  width: fit-content

  &:hover
    border-color: #afb8c1
    box-shadow: inset 0 1px 0 #ffffff, 0 4px 12px rgba(0, 0, 0, 0.08)
    transform: translateY(-1px)

  &:active
    transform: translateY(0)

  &--dense
    padding-block: 4px
    gap: 8px

  &__pill
    display: inline-flex
    align-items: center
    gap: 8px
    padding: 6px 12px
    border-radius: 999px
    background: linear-gradient(180deg, #2ea043, #238636)
    color: #fff
    font-weight: 800
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.75rem
    border: 1px solid #1f6f3d
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2)

  &__value
    display: inline-flex
    align-items: center
    font-weight: 700
    letter-spacing: 0.01em
    padding-right: 4px
    color: #24292f
    white-space: nowrap
</style>
