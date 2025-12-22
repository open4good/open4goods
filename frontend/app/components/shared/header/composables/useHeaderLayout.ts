/**
 * Layout composable for PageHeader component
 *
 * Manages:
 * - CSS classes for variant, layout, and alignment
 * - Dynamic styles (minHeight, paddingY)
 * - Vuetify container props
 * - Responsive behavior
 *
 * @module components/shared/header/composables
 */

import type {
  PageHeaderVariant,
  PageHeaderLayout,
  PageHeaderContentAlign,
  PageHeaderContainer,
} from '../types'

export interface UseHeaderLayoutOptions {
  /** Header variant */
  variant: PageHeaderVariant
  /** Content layout */
  layout: PageHeaderLayout
  /** Content alignment */
  contentAlign: PageHeaderContentAlign
  /** Container size */
  container: PageHeaderContainer
  /** Custom max-width */
  maxWidth?: string
  /** Minimum height CSS value */
  minHeight?: string | null
  /** Vertical padding CSS value */
  paddingY?: string | null
}

/**
 * Generates layout classes, styles, and Vuetify props for page headers
 */
export function useHeaderLayout(options: UseHeaderLayoutOptions) {
  /**
   * Root container CSS classes
   */
  const containerClasses = computed(() => ({
    'page-header': true,
    [`page-header--${options.variant}`]: true,
    [`page-header--layout-${options.layout}`]: true,
    [`page-header--align-${options.contentAlign}`]: true,
  }))

  /**
   * Root container inline styles
   */
  const containerStyles = computed(() => ({
    ...(options.minHeight && { minHeight: options.minHeight }),
    ...(options.paddingY && { paddingBlock: options.paddingY }),
    ...(options.maxWidth && { maxWidth: options.maxWidth }),
  }))

  /**
   * Vuetify v-container props
   */
  const vuetifyContainerProps = computed(() => ({
    fluid: options.container === 'fluid',
    ...(options.container !== 'fluid' && {
      class: `mx-auto`,
      style: {
        maxWidth:
          options.container === 'lg'
            ? '1280px'
            : options.container === 'xl'
              ? '1920px'
              : options.container === 'xxl'
                ? '2560px'
                : undefined,
      },
    }),
  }))

  /**
   * Grid columns for v-row layout
   */
  const gridColumns = computed(() => {
    if (options.layout === 'single-column') {
      return { main: 12, media: 0 }
    }

    if (options.layout === '2-columns') {
      return { main: 7, media: 5 }
    }

    // 3-columns layout
    return { left: 3, main: 6, right: 3 }
  })

  /**
   * Check if layout uses media column
   */
  const hasMediaColumn = computed(
    () => options.layout === '2-columns' || options.layout === '3-columns'
  )

  return {
    containerClasses,
    containerStyles,
    vuetifyContainerProps,
    gridColumns,
    hasMediaColumn,
  }
}
