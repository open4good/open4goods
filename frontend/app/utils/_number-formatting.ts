export type NumberFormatter = (
  value: number,
  options?: Intl.NumberFormatOptions
) => string

type FormatNumericRangeValueOptions = {
  isPrice?: boolean
  fallback?: string
}

const DEFAULT_FALLBACK = '–'

export const formatNumericRangeValue = (
  value: number | string | null | undefined,
  formatter: NumberFormatter,
  {
    isPrice = false,
    fallback = DEFAULT_FALLBACK,
  }: FormatNumericRangeValueOptions = {}
): string => {
  if (value == null || value === '') {
    return fallback
  }

  const numeric = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(numeric)) {
    return typeof value === 'string' ? value : fallback
  }

  const formatOptions: Intl.NumberFormatOptions = isPrice
    ? { maximumFractionDigits: 0, minimumFractionDigits: 0 }
    : { maximumFractionDigits: 1, minimumFractionDigits: 0 }

  return formatter(numeric, formatOptions)
}
