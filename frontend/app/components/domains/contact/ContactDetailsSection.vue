
<script setup lang="ts">
export interface ContactDetailLink {
  label: string
  href: string
  ariaLabel?: string
}

export interface ContactDetailItem {
  icon: string
  title: string
  description: string
  links?: ContactDetailLink[]
}

withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    subtitle: string
    items: ContactDetailItem[]
  }>(),
  {
    eyebrow: undefined,
  }
)


</script>

<style scoped lang="sass">
.contact-details
  background: linear-gradient(180deg, rgb(var(--v-theme-surface-default)) 0%, rgba(var(--v-theme-surface-ice-100), 0.95) 100%)

  &__header
    text-align: center
    max-width: 52rem
    margin: 0 auto 3rem auto
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__eyebrow
    display: inline-flex
    align-self: center
    padding: 0.4rem 1rem
    border-radius: 999px
    background: rgb(var(--v-theme-surface-primary-100))
    color: rgb(var(--v-theme-primary))
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.82rem

  &__title
    font-size: clamp(2rem, 3vw, 2.6rem)
    font-weight: 700
    margin: 0

  &__subtitle
    --subtitle-size: 1.05rem
    margin: 0

  &__cards
    margin-top: 1.5rem

  &__card
    display: flex
    flex-direction: column
    gap: 1rem
    padding: 2rem
    background-color: rgb(var(--v-theme-surface-glass))
    backdrop-filter: blur(6px)
    border: 1px solid rgb(var(--v-theme-surface-primary-080))
    transition: transform 0.2s ease, box-shadow 0.2s ease

    &:hover
      transform: translateY(-6px)
      box-shadow: 0 18px 40px -24px rgba(var(--v-theme-shadow-primary-600), 0.6)

  &__icon-wrapper
    display: flex
    justify-content: flex-start

  &__icon
    background-color: rgb(var(--v-theme-surface-primary-120))

  &__content
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__card-title
    font-size: 1.35rem
    font-weight: 600
    margin: 0

  &__card-description
    color: rgba(var(--v-theme-text-neutral-strong), 0.75)
    font-size: 1rem
    line-height: 1.6
    margin: 0

  &__links
    display: flex
    flex-wrap: wrap
    gap: 0.75rem

@media (max-width: 959px)
  .contact-details
    &__card
      text-align: center

    &__icon-wrapper
      justify-content: center

    &__links
      justify-content: center
</style>
