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
  memberVariant?: 'core' | 'contributor'
}

const props = withDefaults(defineProps<Props>(), {
  members: () => [],
  variant: 'light',
  id: undefined,
  descriptionBlocId: undefined,
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
    <v-container class="py-12 px-4 mx-auto" max-width="xl">
      <div class="team-members-section__header">
        <h2 :id="headingId" class="team-members-section__title">
          {{ props.title }}
        </h2>

        <div v-if="props.descriptionBlocId" class="team-members-section__intro">
          <TextContent :bloc-id="props.descriptionBlocId" :ipsum-length="100" />
        </div>
      </div>

      <v-row
        v-if="hasMembers"
        class="team-members-section__grid mt-2"
        align="stretch"
        justify="center"
      >
        <v-col
          v-for="member in props.members"
          :key="member.name"
          cols="12"
          sm="6"
          lg="4"
          xl="3"
          class="d-flex"
        >
          <TeamMemberCard
            :member="member"
            :variant="props.memberVariant"
            class="flex-grow-1"
          />
        </v-col>
      </v-row>

      <v-alert v-else type="info" variant="tonal" border="start" class="mt-6">
        {{ t('team.sections.empty') }}
      </v-alert>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.team-members-section
  position: relative

  &--light
    background: linear-gradient(180deg, rgba(var(--v-theme-surface-alt), 1) 0%, rgba(var(--v-theme-surface-default), 0.9) 100%)

  &--muted
    background: linear-gradient(180deg, rgba(var(--v-theme-surface-muted), 0.95) 0%, rgba(var(--v-theme-surface-primary-050), 0.95) 100%)

  &__header
    text-align: center
    margin: 0 auto 3rem
    display: flex
    flex-direction: column
    gap: 1rem



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
