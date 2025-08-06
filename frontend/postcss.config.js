import prefixer from 'postcss-prefix-selector'

export default {
  plugins: [
    prefixer({
      prefix: '.xwiki-sandbox',
      transform: (prefix, selector, prefixedSelector) => {
        if (
          selector.startsWith('html') ||
          selector.startsWith('body') ||
          selector === 'body' ||
          selector === 'html' ||
          selector.startsWith(prefix)
        ) {
          return selector
        }
        return prefixedSelector
      }
    })
  ]
}
