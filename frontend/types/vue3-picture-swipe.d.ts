declare module 'vue3-picture-swipe' {
  import type { DefineComponent } from 'vue'

  interface PictureSwipeItem {
    src: string
    msrc?: string
    width?: number
    height?: number
    title?: string
    html?: string
    el?: HTMLElement
  }

  interface PictureSwipeOptions {
    loop?: boolean
    allowPanToNext?: boolean
    spacing?: number
    bgOpacity?: number
    mouseUsed?: boolean
    escKey?: boolean
    arrowKeys?: boolean
    history?: boolean
    galleryUID?: string
    gallerySelector?: string | false
    getThumbBoundsFn?: (index: number) => { x: number; y: number; w: number }
  }

  const VuePictureSwipe: DefineComponent<{
    items: PictureSwipeItem[]
    options?: PictureSwipeOptions
    openOn?: number | null
    isOpen?: boolean
  }>

  export type { PictureSwipeItem, PictureSwipeOptions }
  export default VuePictureSwipe
}
