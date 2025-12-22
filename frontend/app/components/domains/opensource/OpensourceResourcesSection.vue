<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface ResourceLink {
  icon: string
  title: string
  descriptionBlocId: string
  href: string
  ariaLabel: string
  target?: string
  rel?: string
}

interface ContactCta {
  title: string
  descriptionBlocId: string
  ctaLabel: string
  ctaHref: string
  ctaAriaLabel: string
}

interface OpendataCallout {
  title: string
  description: string
  ctaLabel: string
  ctaHref: string
  ctaAriaLabel: string
}

interface PromptCallout {
  title: string
  description: string
  ctaLabel: string
  ctaHref: string
  ctaAriaLabel: string
}

withDefaults(
  defineProps<{
    eyebrow: string
    title: string
    descriptionBlocId: string
    resources: ResourceLink[]
    contact: ContactCta
    opendataCallout?: OpendataCallout
    promptCallout?: PromptCallout
  }>(),
  {
    opendataCallout: undefined,
    promptCallout: undefined,
  }
)
</script>

<template>
  <section
    class="opensource-resources"
    aria-labelledby="opensource-resources-title"
  >
    <v-container class="py-14">
      <div class="section-header">
        <v-chip
          v-if="eyebrow"
          label
          size="small"
          color="accent-supporting"
          class="section-chip"
        >
          {{ eyebrow }}
        </v-chip>
        <h2 id="opensource-resources-title" class="section-title">
          {{ title }}
        </h2>
        <TextContent :bloc-id="descriptionBlocId" :ipsum-length="200" />
      </div>

      <v-row class="mt-8" align="stretch" dense>
        <v-col
          v-for="resource in resources"
          :key="resource.href"
          cols="12"
          md="4"
          class="d-flex"
        >
          <v-card class="resource-card" rounded="xl" elevation="4">
            <div class="resource-icon" aria-hidden="true">
              <v-icon :icon="resource.icon" size="36" />
            </div>
            <div class="resource-body">
              <h3 class="resource-title">{{ resource.title }}</h3>
              <TextContent
                :bloc-id="resource.descriptionBlocId"
                :ipsum-length="140"
              />
            </div>
            <v-btn
              :href="resource.href"
              :aria-label="resource.ariaLabel"
              :target="resource.target"
              :rel="resource.rel"
              variant="text"
              color="primary"
              append-icon="mdi-arrow-top-right"
              class="mt-auto align-self-start"
            >
              {{ resource.title }}
            </v-btn>
          </v-card>
        </v-col>
      </v-row>

      <v-card
        v-if="promptCallout"
        class="opendata-card"
        rounded="xl"
        elevation="8"
        role="region"
        :aria-label="promptCallout.title"
      >
        <div class="opendata-content">
          <div class="opendata-text">
            <h2 class="opendata-title">{{ promptCallout.title }}</h2>
            <p class="opendata-description">
              {{ promptCallout.description }}
            </p>
          </div>
          <v-btn
            :href="promptCallout.ctaHref"
            color="secondary"
            variant="flat"
            size="large"
            :aria-label="promptCallout.ctaAriaLabel"
            append-icon="mdi-creation"
          >
            {{ promptCallout.ctaLabel }}
          </v-btn>
        </div>
      </v-card>

      <v-card
        v-if="opendataCallout"
        class="opendata-card"
        rounded="xl"
        elevation="8"
        role="region"
        :aria-label="opendataCallout.title"
      >
        <div class="opendata-content">
          <div class="opendata-text">
            <h2 class="opendata-title">{{ opendataCallout.title }}</h2>
            <p class="opendata-description">
              {{ opendataCallout.description }}
            </p>
          </div>
          <v-btn
            :href="opendataCallout.ctaHref"
            color="primary"
            variant="flat"
            size="large"
            :aria-label="opendataCallout.ctaAriaLabel"
            append-icon="mdi-arrow-right"
          >
            {{ opendataCallout.ctaLabel }}
          </v-btn>
        </div>
      </v-card>

      <v-card class="contact-card" rounded="xl" elevation="8">
        <div class="contact-content">
          <div class="contact-text">
            <h2 class="contact-title">{{ contact.title }}</h2>
            <TextContent
              :bloc-id="contact.descriptionBlocId"
              :ipsum-length="180"
            />
          </div>
          <v-btn
            :href="contact.ctaHref"
            color="accent-supporting"
            variant="flat"
            size="large"
            :aria-label="contact.ctaAriaLabel"
            append-icon="mdi-arrow-right"
          >
            {{ contact.ctaLabel }}
          </v-btn>
        </div>
      </v-card>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opensource-resources
  background: rgba(var(--v-theme-surface-default), 1)
  content-visibility: auto
  contain-intrinsic-size: 800px

.section-header
  max-width: 760px
  margin: 0 auto
  text-align: center
  display: flex
  flex-direction: column
  gap: 1rem

.section-chip
  align-self: center
  font-weight: 600
  letter-spacing: 0.08em

.section-title
  font-size: clamp(2rem, 3.5vw, 2.75rem)
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.resource-card
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.5rem, 3vw, 2rem)
  background: rgba(var(--v-theme-surface-glass), 0.95)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)

.resource-icon
  width: 54px
  height: 54px
  border-radius: 16px
  background: rgba(var(--v-theme-surface-primary-080), 0.95)
  display: grid
  place-items: center
  color: rgba(var(--v-theme-accent-primary-highlight), 1)

.resource-body
  display: flex
  flex-direction: column
  gap: 0.75rem

.resource-title
  font-size: 1.25rem
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-card
  margin-top: 3.5rem
  padding: clamp(1.75rem, 3vw, 2.5rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.96)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)

.opendata-content
  display: flex
  flex-direction: column
  gap: 1.5rem

@media (min-width: 960px)
  .opendata-content
    flex-direction: row
    align-items: center
    justify-content: space-between

.opendata-text
  display: flex
  flex-direction: column
  gap: 0.75rem

.opendata-title
  font-size: 1.6rem
  margin: 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-description
  margin: 0
  color: rgba(var(--v-theme-text-neutral-secondary), 1)

.contact-card
  margin-top: 4rem
  padding: clamp(2rem, 4vw, 3rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-callout-start), 0.95), rgba(var(--v-theme-surface-callout-end), 0.95))
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3)

.contact-content
  display: flex
  flex-direction: column
  gap: 1.5rem

@media (min-width: 960px)
  .contact-content
    flex-direction: row
    align-items: center
    justify-content: space-between

.contact-title
  font-size: 1.8rem
  margin: 0 0 0.5rem 0
  color: rgba(var(--v-theme-text-neutral-strong), 1)
</style>
