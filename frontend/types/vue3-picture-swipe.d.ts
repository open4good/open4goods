declare module 'vue3-picture-swipe' {
  import type { DefineComponent } from 'vue'

  export interface VuePictureSwipeItem {
    src: string
    thumbnail?: string
    w: number
    h: number
    title?: string
    alt?: string
    html?: string
    htmlAfterThumbnail?: string
    [key: string]: unknown
  }

  export interface VuePictureSwipeOptions {
    [key: string]: unknown
  }

  const VuePictureSwipe: DefineComponent<{ items: VuePictureSwipeItem[]; options?: VuePictureSwipeOptions }>
  export default VuePictureSwipe
}
