<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'
import TextContent from '~/components/domains/content/TextContent.vue'
import TeamMemberCard from './TeamMemberCard.vue'
import type { Member } from '~~/shared/api-client'

interface Props {
  title: string
  members?: Member[]
  descriptionBlocId?: string
  variant?: 'light' | 'muted'
  id?: string
  eyebrow?: string
  memberVariant?: 'core' | 'contributor'
}

const props = withDefaults(defineProps<Props>(), {
  members: () => [],
  variant: 'light',
  id: undefined,
  descriptionBlocId: undefined,
  eyebrow: undefined,
  memberVariant: 'core',
})

const generatedId = useId()
const headingId = computed(() => props.id ?? generatedId)
const regionLabelledBy = computed(() => headingId.value)

const hasMembers = computed(() => props.members.length > 0)
const { t } = useI18n()
</script>

<template>
  <section
    :id="props.id"
    class="team-members-section"
    :class="[`team-members-section--${props.variant}`]"
    role="region"
    :aria-labelledby="regionLabelledBy"
  >
    <v-container class="py-12">
      <div class="team-members-section__header">
        <v-chip
          v-if="props.eyebrow"
          class="team-members-section__eyebrow"
          color="primary"
          size="small"
          label
          variant="tonal"
        >
          {{ props.eyebrow }}
        </v-chip>

        <h2 :id="headingId" class="team-members-section__title">
          {{ props.title }}
        </h2>

        <div v-if="props.descriptionBlocId" class="team-members-section__intro">
          <TextContent :bloc-id="props.descriptionBlocId" :ipsum-length="100" />
        </div>
      </div>

      <v-row v-if="hasMembers" class="team-members-section__grid" align="stretch" justify="center">
        <v-col
          v-for="member in props.members"
          :key="member.name"
          cols="12"
          sm="6"
          lg="4"
          xl="3"
          class="d-flex"
        >
          <TeamMemberCard :member="member" :variant="props.memberVariant" class="flex-grow-1" />
        </v-col>
      </v-row>

      <v-alert
        v-else
        type="info"
        variant="tonal"
        border="start"
        class="mt-6"
      >
        {{ t('team.sections.empty') }}
      </v-alert>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.team-members-section
  position: relative
  padding-inline: clamp(1rem, 4vw, 4rem)

  &--light
    background: linear-gradient(180deg, rgba(255, 255, 255, 1) 0%, rgba(245, 250, 255, 0.8) 100%)

  &--muted
    background: linear-gradient(180deg, rgba(248, 250, 252, 0.95) 0%, rgba(238, 244, 250, 0.95) 100%)

  &__header
    text-align: center
    max-width: 720px
    margin: 0 auto 3rem
    display: flex
    flex-direction: column
    gap: 1rem

  &__eyebrow
    align-self: center
    letter-spacing: 0.08em
    text-transform: uppercase

  &__title
    font-size: clamp(1.8rem, 3vw, 2.5rem)
    font-weight: 700
    margin: 0

  &__intro :deep(.text-content)
    padding: 0

  &__intro :deep(.xwiki-sandbox)
    font-size: 1.05rem
    line-height: 1.7

  &__grid
    gap: 2.5rem 1.5rem
</style>
