declare module '#app' {
  interface PageMeta {
    /** `false` keeps the server-rendered HTML out of shared caches (GOU-2700). */
    htmlCache?: boolean
  }
}

export {}
