const LOREM_SENTENCES = [
  'Lorem ipsum dolor sit amet, consectetur adipiscing elit.',
  'Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
  'Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.',
  'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.',
  'Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
  'Curabitur blandit tempus porttitor, integer posuere erat a ante venenatis dapibus posuere velit aliquet.',
  'Maecenas faucibus mollis interdum, vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.',
  'Praesent commodo cursus magna, vel scelerisque nisl consectetur et.',
]

export const DEFAULT_LOREM_LENGTH = 480

/**
 * Generates deterministic lorem ipsum paragraphs until the plain text length
 * meets or exceeds the requested target.
 */
export const _generateLoremIpsum = (targetLength: number) => {
  const safeLength = Math.max(1, Math.floor(targetLength))
  const sentences: [string, ...string[]] =
    LOREM_SENTENCES.length > 0
      ? (LOREM_SENTENCES as [string, ...string[]])
      : ['Lorem ipsum dolor sit amet.']
  const paragraphs: string[] = []
  let accumulatedLength = 0
  let sentenceIndex = 0
  let currentParagraph: string[] = []

  while (accumulatedLength < safeLength) {
    const index = sentenceIndex % sentences.length
    const sentence = sentences[index] ?? sentences[0]
    currentParagraph.push(sentence)
    accumulatedLength += sentence.length + 1
    sentenceIndex += 1

    const hasMinimumSentences = currentParagraph.length >= 3
    const reachedTarget = accumulatedLength >= safeLength
    const reachedMaximumSentences = currentParagraph.length >= 5

    if ((hasMinimumSentences && reachedTarget) || reachedMaximumSentences) {
      paragraphs.push(currentParagraph.join(' '))
      currentParagraph = []
    }
  }

  if (currentParagraph.length) {
    paragraphs.push(currentParagraph.join(' '))
  }

  return paragraphs.map(paragraph => `<p>${paragraph}</p>`).join('')
}
