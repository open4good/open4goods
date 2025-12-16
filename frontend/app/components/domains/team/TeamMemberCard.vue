<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Member } from '~~/shared/api-client'
import { _slugify } from '~~/shared/utils/_slugify'

interface Props {
  member: Member
  variant?: 'core' | 'contributor'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'core',
})

const { t } = useI18n()

const titleId = useId()
const descriptionId = useId()

const displayName = computed(
  () => props.member.name?.trim() || t('team.cards.unknownName')
)

const baseBlocId = computed(() => {
  const explicit = props.member.blocId?.trim()
  if (explicit) {
    return explicit
  }

  const slug = _slugify(props.member.name ?? '')
  if (!slug) {
    return ''
  }

  return `pages:team:${slug}:`
})

const titleBlocId = computed(() => {
  const base = baseBlocId.value
  if (!base) {
    return ''
  }

  return base.endsWith(':') ? `${base.slice(0, -1)}-title:` : `${base}-title`
})

const portraitAlt = computed(() =>
  t('team.cards.portraitAlt', { name: displayName.value })
)

const linkedInLabel = computed(() =>
  t('team.cards.linkedIn', { name: displayName.value })
)

const hasLinkedIn = computed(() => Boolean(props.member.linkedInUrl))
</script>

<template>
  <v-card
    class="team-member-card"
    elevation="4"
    rounded="xl"
    role="article"
    :aria-labelledby="titleId"
    :aria-describedby="descriptionId"
  >
    <div class="team-member-card__avatar" aria-hidden="true">
      <NuxtImg
        :src="props.member.imageUrl || '/nudger-icon-512x512.png'"
        :alt="portraitAlt"
        width="128"
        height="128"
        class="team-member-card__image"
        loading="lazy"
      />
    </div>

    <v-card-item>
      <v-card-title :id="titleId" class="team-member-card__name">
        {{ displayName }}
      </v-card-title>
      <v-card-subtitle class="team-member-card__title">
        <TextContent :bloc-id="titleBlocId" :ipsum-length="8" />
      </v-card-subtitle>
    </v-card-item>

    <v-card-text :id="descriptionId" class="team-member-card__bio">
      <TextContent :bloc-id="baseBlocId" :ipsum-length="60" />
    </v-card-text>

    <v-card-actions class="team-member-card__actions">
      <v-btn
        v-if="hasLinkedIn"
        :href="props.member.linkedInUrl"
        target="_blank"
        rel="noopener noreferrer"
        color="primary"
        variant="tonal"
        prepend-icon="mdi-linkedin"
        :aria-label="linkedInLabel"
      >
        {{ t('team.cards.connect') }}
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<style scoped lang="sass">
.team-member-card
  display: flex
  flex-direction: column
  align-items: center
  text-align: center
  padding-top: 2.5rem
  background: rgb(var(--v-theme-surface))
  height: 100%
  position: relative
  overflow: visible

  &__avatar
    position: absolute
    top: -3.5rem
    left: 50%
    transform: translateX(-50%)
    width: 112px
    height: 112px
    border-radius: 50%
    border: 6px solid rgb(var(--v-theme-surface))
    box-shadow: 0 12px 28px rgba(var(--v-theme-accent-primary-highlight), 0.25)
    background: linear-gradient(135deg, rgba(var(--v-theme-accent-primary-highlight), 0.15), rgba(var(--v-theme-accent-supporting), 0.15))
    display: flex
    align-items: center
    justify-content: center

  &__image
    border-radius: 50%
    width: 100%
    height: 100%
    object-fit: cover

  &__name
    margin-top: 3.75rem
    font-weight: 700
    font-size: 1.25rem

  &__title :deep(.text-content)
    padding: 0
    margin-top: 0.25rem
    color: rgba(var(--v-theme-on-surface), var(--v-medium-emphasis-opacity))

  &__title :deep(.xwiki-sandbox)
    font-size: 0.95rem

  &__bio
    margin-top: 0.75rem

  &__bio :deep(.text-content)
    padding: 0

  &__bio :deep(.xwiki-sandbox)
    font-size: 0.95rem
    line-height: 1.6

  &__actions
    justify-content: center
    padding-bottom: 1.5rem
</style>
