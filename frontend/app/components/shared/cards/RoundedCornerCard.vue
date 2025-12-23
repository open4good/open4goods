<script setup lang="ts">
import { computed, ref, useSlots } from 'vue'
import { useI18n } from 'vue-i18n'
import { useHeroBackgroundAsset } from '~~/app/composables/useThemedAsset'

export type CornerSize = 'sm' | 'md' | 'lg' | 'xl'
export type CornerVariant = 'icon' | 'text' | 'custom' | 'none'
export type AccentCorner =
  | 'top-left'
  | 'top-right'
  | 'bottom-left'
  | 'bottom-right'
export type SurfaceTone = 'glass' | 'strong' | 'hero'
export type RoundedSize = 'sm' | 'md' | 'lg' | 'xl'

const cornerSizeTokens: Record<CornerSize, string> = {
  sm: '46px',
  md: '58px',
  lg: '72px',
  xl: '120px',
}

const roundedSizeTokens: Record<RoundedSize, string> = {
  sm: '14px',
  md: '18px',
  lg: '24px',
  xl: '32px',
}

const props = withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    eyebrow?: string
    accentCorner?: AccentCorner
    cornerSize?: CornerSize
    cornerVariant?: CornerVariant
    cornerLabel?: string
    cornerIcon?: string
    selected?: boolean
    selectable?: boolean
    selectedIcon?: string
    unselectedIcon?: string
    rounded?: RoundedSize
    surface?: SurfaceTone
    elevation?: number
    hoverElevation?: number
    ariaLabel?: string
    disabled?: boolean
    cornerTooltip?: string
  }>(),
  {
    title: '',
    subtitle: '',
    eyebrow: '',
    accentCorner: 'bottom-right',
    cornerSize: 'md',
    cornerVariant: 'icon',
    cornerLabel: '',
    cornerIcon: '',
    selected: false,
    selectable: true,
    selectedIcon: '/icons/rounded-card-selected.svg',
    unselectedIcon: '/icons/rounded-card-unselected.svg',
    rounded: 'md',
    surface: 'glass',
    elevation: 8,
    hoverElevation: 12,
    ariaLabel: '',
    disabled: false,
    cornerTooltip: '',
  }
)

type RoundedCornerCardEmits = {
  (event: 'update:selected' | 'select', value: boolean): void
  (event: 'click', value: MouseEvent): void
}

const emit = defineEmits<RoundedCornerCardEmits>()

const slots = useSlots()
const { t } = useI18n()
const heroBackgroundAsset = useHeroBackgroundAsset()

const isInteractive = computed(() => props.selectable && !props.disabled)
const tabIndex = computed(() => (props.disabled ? -1 : 0))
const ariaPressed = computed(() =>
  isInteractive.value ? String(props.selected) : undefined
)
const ariaDisabled = computed(() => (props.disabled ? 'true' : undefined))

const surfaceClass = computed(() => `rounded-card--surface-${props.surface}`)
const cornerClass = computed(() => `rounded-card--corner-${props.accentCorner}`)
const roundedClass = computed(() => `rounded-card--rounded-${props.rounded}`)

const sizeStyles = computed(() => {
  const styles: Record<string, string> = {
    '--rounded-card-corner-size': cornerSizeTokens[props.cornerSize],
    '--rounded-card-radius': roundedSizeTokens[props.rounded],
  }

  if (props.surface === 'hero' && heroBackgroundAsset.value) {
    styles['--rounded-card-bg-image'] = `url('${heroBackgroundAsset.value}')`
  }

  return styles
})

const hasCornerSlot = computed(() => Boolean(slots.corner))
const resolvedCornerVariant = computed<CornerVariant>(() => {
  if (props.cornerVariant === 'custom' && !hasCornerSlot.value) {
    return 'icon'
  }

  return props.cornerVariant
})

const resolvedCornerLabel = computed(() => {
  if (props.cornerLabel) {
    return props.cornerLabel
  }

  return props.selected
    ? t('shared.roundedCard.cornerSelected')
    : t('shared.roundedCard.cornerIdle')
})

const cornerTooltip = computed(() => {
  if (props.cornerTooltip) {
    return props.cornerTooltip
  }

  if (!props.selectable) {
    return ''
  }

  return props.selected
    ? t('shared.roundedCard.cornerSelected')
    : t('shared.roundedCard.cornerIdle')
})

const resolvedIcon = computed(() =>
  props.selected ? props.selectedIcon : props.unselectedIcon
)

const hoverState = ref(false)
const isHovered = computed({
  get: () => hoverState.value,
  set: (value: boolean) => {
    hoverState.value = value
  },
})
const elevationValue = computed(() =>
  isHovered.value ? props.hoverElevation : props.elevation
)

const handleSelect = (event: MouseEvent | KeyboardEvent) => {
  emit('click', event as MouseEvent)

  if (!isInteractive.value) {
    return
  }

  const nextValue = !props.selected
  emit('update:selected', nextValue)
  emit('select', nextValue)
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    handleSelect(event)
  }
}

const cardAriaLabel = computed(
  () => props.ariaLabel || props.title || props.subtitle || undefined
)

const hasHeader = computed(() =>
  Boolean(props.title || props.subtitle || props.eyebrow || slots.header)
)
</script>

<template>
  <v-card
    class="rounded-card"
    :class="[
      surfaceClass,
      cornerClass,
      roundedClass,
      {
        'rounded-card--selected': selected,
        'rounded-card--clickable': isInteractive,
        'rounded-card--disabled': disabled,
      },
    ]"
    variant="elevated"
    :elevation="elevationValue"
    :style="sizeStyles"
    :tabindex="tabIndex"
    :role="isInteractive ? 'button' : 'group'"
    :aria-pressed="ariaPressed"
    :aria-disabled="ariaDisabled"
    :aria-label="cardAriaLabel"
    :ripple="isInteractive"
    @click="handleSelect"
    @keydown="handleKeyDown"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @focusin="isHovered = true"
    @focusout="isHovered = false"
  >
    <div
      class="rounded-card__corner"
      :class="[`rounded-card__corner--${resolvedCornerVariant}`]"
      :title="cornerTooltip"
    >
      <div
        v-if="resolvedCornerVariant === 'icon'"
        class="rounded-card__corner-indicator"
        aria-hidden="true"
      >
        <v-icon
          v-if="resolvedIcon?.startsWith('mdi-')"
          :icon="resolvedIcon"
          size="26"
        />
        <v-img
          v-else-if="resolvedIcon"
          :src="resolvedIcon"
          :alt="cornerTooltip"
          eager
          cover
        />
      </div>
      <div
        v-else-if="resolvedCornerVariant === 'text'"
        class="rounded-card__corner-label"
      >
        <v-icon v-if="cornerIcon" :icon="cornerIcon" size="18" />
        <span>{{ resolvedCornerLabel }}</span>
      </div>
      <div
        v-else-if="resolvedCornerVariant === 'custom'"
        class="rounded-card__corner-custom"
      >
        <slot name="corner" />
      </div>
    </div>

    <div class="rounded-card__body">
      <header v-if="hasHeader" class="rounded-card__header">
        <slot name="header">
          <p v-if="eyebrow" class="rounded-card__eyebrow">{{ eyebrow }}</p>
          <p v-if="title" class="rounded-card__title">{{ title }}</p>
          <p v-if="subtitle" class="rounded-card__subtitle">{{ subtitle }}</p>
        </slot>
      </header>

      <div class="rounded-card__content">
        <slot />
      </div>

      <footer v-if="$slots.actions" class="rounded-card__actions">
        <slot name="actions" />
      </footer>
    </div>
  </v-card>
</template>

<style scoped lang="scss">
.rounded-card {
  --rounded-card-border: rgba(var(--v-theme-border-primary-strong), 0.7);
  --rounded-card-highlight-main: rgba(var(--v-theme-hero-gradient-start), 0.38);
  --rounded-card-highlight-alt: rgba(var(--v-theme-hero-gradient-end), 0.32);
  --rounded-card-text: rgb(var(--v-theme-text-neutral-strong));
  --rounded-card-bg: rgba(var(--v-theme-surface-glass), 0.96);
  --rounded-card-corner-size: 58px;
  --rounded-card-radius: 18px;

  position: relative;
  display: flex;
  padding: clamp(1rem, 3vw, 1.5rem) clamp(1.1rem, 3vw, 1.6rem);
  border-radius: var(--rounded-card-radius);
  border: 1px solid var(--rounded-card-border);
  background: var(--rounded-card-bg);
  color: var(--rounded-card-text);
  overflow: hidden;
  transition:
    box-shadow 180ms ease,
    transform 180ms ease,
    border-color 180ms ease;

  &--surface-strong {
    --rounded-card-bg: rgba(var(--v-theme-surface-glass-strong), 0.98);
  }

  &--surface-hero {
    --rounded-card-bg: rgba(var(--v-theme-surface-glass), 0.85);
    background-image: var(--rounded-card-bg-image);
    background-size: cover;
    background-position: center;
    color: rgb(var(--v-theme-text-light-strong));
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.35);

    &::before {
      background: linear-gradient(
        160deg,
        rgba(var(--v-theme-surface-default), 0.1) 0%,
        rgba(var(--v-theme-surface-default), 0.4) 100%
      );
      backdrop-filter: blur(0px); /* Reset blur if needed */
    }
  }

  &--selected {
    --rounded-card-border: rgba(var(--v-theme-accent-supporting), 0.75);
    box-shadow: 0 16px 36px rgba(var(--v-theme-shadow-primary-600), 0.2);
  }

  &--clickable:not(&--disabled) {
    cursor: pointer;

    &:hover,
    &:focus-visible {
      transform: translateY(-2px);
      box-shadow: 0 18px 42px rgba(var(--v-theme-shadow-primary-600), 0.16);
    }
  }

  &--disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(
        circle at 22% 18%,
        rgba(var(--v-theme-surface-primary-080), 0.5),
        transparent 40%
      ),
      radial-gradient(
        circle at 80% 80%,
        rgba(var(--v-theme-surface-primary-100), 0.45),
        transparent 40%
      );
    opacity: 0.85;
    pointer-events: none;
  }

  &__body {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: clamp(0.7rem, 1.8vw, 0.95rem);
    z-index: 1;
  }

  &__header {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
  }

  &__eyebrow {
    margin: 0;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 700;
    color: rgba(var(--v-theme-hero-gradient-end), 0.85);
  }

  &__title {
    margin: 0;
    font-weight: 700;
    font-size: 1.15rem;
    line-height: 1.25;
  }

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    line-height: 1.35;
    font-weight: 500;
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  &__actions {
    margin-top: 0.35rem;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 0.75rem;
  }

  &__corner {
    position: absolute;
    width: var(--rounded-card-corner-size);
    height: var(--rounded-card-corner-size);
    background: linear-gradient(
      135deg,
      var(--rounded-card-highlight-main),
      rgba(var(--v-theme-accent-supporting), 0.38)
    );
    border: 1px solid var(--rounded-card-border);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: rgb(var(--v-theme-text-on-accent));
    overflow: hidden;
    z-index: 2;
    transition:
      transform 160ms ease,
      border-color 160ms ease,
      background 160ms ease;
    backdrop-filter: blur(6px);

    &--icon {
      padding: 0.35rem;
    }

    &--text {
      padding: 0.45rem 0.65rem;
    }

    &--custom {
      padding: 0.3rem;
    }

    &--none {
      display: none;
    }
  }

  &__corner-indicator {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
  }

  &__corner-label {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-weight: 700;
    font-size: 0.85rem;
  }

  &__corner-custom {
    width: 100%;
    height: 100%;
    display: grid;
    place-items: center;
  }

  &--corner-bottom-right &__corner {
    bottom: 0;
    right: 0;
    border-radius: 54% 0 0 0;
  }

  &--corner-bottom-left &__corner {
    bottom: 0;
    left: 0;
    border-radius: 0 54% 0 0;
  }

  &--corner-top-right &__corner {
    top: 0;
    right: 0;
    border-radius: 0 0 0 54%;
  }

  &--corner-top-left &__corner {
    top: 0;
    left: 0;
    border-radius: 0 0 54% 0;
  }

  &--rounded-sm {
    --rounded-card-radius: 14px;
  }

  &--rounded-md {
    --rounded-card-radius: 18px;
  }

  &--rounded-lg {
    --rounded-card-radius: 24px;
  }
}

@media (max-width: 600px) {
  .rounded-card {
    padding: clamp(0.9rem, 3vw, 1.2rem);
  }

  .rounded-card__title {
    font-size: 1.05rem;
  }

  .rounded-card__corner {
    --rounded-card-corner-size: 52px;
  }
}
</style>
