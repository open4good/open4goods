/** True when the path is a blog article page (`/blog/<slug>`), not another route reusing `params.slug`. */
export const isBlogArticlePath = (path: string): boolean =>
  /^\/blog\/[^/]+/.test(path)
