export const ACCESSIBILITY_LAYOUT_CLASS = 'accessibility-layout'
export const ACCESSIBILITY_LAYOUT_WIDE_CLASS = 'accessibility-layout--wide'
export const ACCESSIBILITY_REDUCED_MOTION_CLASS = 'accessibility-reduced-motion'

export const applyAccessibilityLayout = (isZoomed: boolean) => {
  if (typeof document === 'undefined') {
    return
  }

  const rootElement = document.documentElement

  rootElement.style.fontSize = isZoomed ? '120%' : ''
  rootElement.classList.toggle(ACCESSIBILITY_LAYOUT_CLASS, isZoomed)
  rootElement.classList.toggle(ACCESSIBILITY_LAYOUT_WIDE_CLASS, isZoomed)
  rootElement.classList.toggle(ACCESSIBILITY_REDUCED_MOTION_CLASS, isZoomed)
}
