export const xwikiSandboxPrefixerOptions = {
  prefix: '.xwiki-sandbox',
  transform(prefix, selector, prefixedSelector) {
    if (selector.startsWith(prefix)) {
      return selector
    }
    if (/^(html|body)/.test(selector)) {
      return selector.replace(/^(html|body)/, prefix)
    }
    return prefixedSelector
  },
}

export default xwikiSandboxPrefixerOptions
