declare module 'postcss-prefix-selector' {
  interface PostcssPlugin {
    (root: unknown, result: unknown): void | Promise<void>
    postcssPlugin: string
  }

  type SelectorMatcher = string | RegExp | Array<string | RegExp>

  interface PostcssPrefixSelectorOptions {
    prefix: string
    transform?: (prefix: string, selector: string, prefixedSelector: string) => string
    exclude?: SelectorMatcher
    include?: SelectorMatcher
    ignoreFiles?: SelectorMatcher
    includeFiles?: SelectorMatcher
  }

  const postcssPrefixSelector: (options: PostcssPrefixSelectorOptions) => PostcssPlugin

  export default postcssPrefixSelector
}
