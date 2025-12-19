export const ACCESSIBILITY_LAYOUT_CLASS = 'accessibility-layout'
export const ACCESSIBILITY_LAYOUT_FLUID_CLASS = 'accessibility-layout--fluid'

export const applyAccessibilityLayout = (isZoomed: boolean) => {
  if (typeof document === 'undefined') {
    return
  }

  const rootElement = document.documentElement

  rootElement.style.fontSize = isZoomed ? '120%' : ''
  rootElement.classList.toggle(ACCESSIBILITY_LAYOUT_CLASS, isZoomed)
  rootElement.classList.toggle(ACCESSIBILITY_LAYOUT_FLUID_CLASS, isZoomed)
}
