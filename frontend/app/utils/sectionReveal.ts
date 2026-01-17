type RevealStyleOptions = {
  baseDelay?: number
  step?: number
}

export const buildRevealStyle = (
  index: number,
  options: RevealStyleOptions = {}
) => {
  const baseDelay = options.baseDelay ?? 0
  const step = options.step ?? 80
  const delay = Math.max(0, baseDelay + index * step)

  return {
    '--reveal-delay': `${delay}ms`,
  }
}
