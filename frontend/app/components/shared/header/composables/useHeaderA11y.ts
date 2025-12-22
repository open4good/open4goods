/**
 * Accessibility composable for PageHeader component
 *
 * Manages:
 * - ARIA attributes (role, aria-labelledby, aria-describedby)
 * - Unique IDs for headings
 * - Keyboard navigation support
 * - Screen reader announcements
 *
 * @module components/shared/header/composables
 */

export interface UseHeaderA11yOptions {
  /** Custom heading ID */
  headingId?: string
  /** Accessible region label */
  ariaLabel?: string
  /** ID of element describing this region */
  ariaDescribedBy?: string
  /** Accessible label for CTA button group */
  ctaGroupLabel?: string
}

/**
 * Generates accessibility attributes and helpers for page headers
 */
export function useHeaderA11y(options: UseHeaderA11yOptions) {
  // Generate unique ID for heading if not provided
  const headingLabelId = computed(() => options.headingId ?? useId())

  /**
   * ARIA attributes for the header region element
   */
  const regionAttrs = computed(() => ({
    role: 'region' as const,
    'aria-labelledby': headingLabelId.value,
    ...(options.ariaDescribedBy && {
      'aria-describedby': options.ariaDescribedBy,
    }),
    ...(options.ariaLabel && { 'aria-label': options.ariaLabel }),
  }))

  /**
   * ARIA attributes for the CTA button group
   */
  const ctaGroupAttrs = computed(() =>
    options.ctaGroupLabel
      ? {
          role: 'group' as const,
          'aria-label': options.ctaGroupLabel,
        }
      : {}
  )

  /**
   * Skip to content link (for keyboard navigation)
   * Can be used to jump past header to main content
   */
  const skipLinkId = computed(() => `skip-${headingLabelId.value}`)

  return {
    headingLabelId,
    regionAttrs,
    ctaGroupAttrs,
    skipLinkId,
  }
}
