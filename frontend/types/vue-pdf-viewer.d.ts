declare module '@vue-pdf-viewer/viewer' {
  import type { DefineComponent, Plugin } from 'vue'
  export const VPdfViewer: DefineComponent<{ src: string }>
  const plugin: Plugin
  export default plugin
}
