/**
 * Normalizes a timestamp to milliseconds.
 * Heuristic: If the numeric value is less than 100 billion (1e11), it is treated as seconds.
 * 1e11 ms is roughly year 1973.
 * 1e11 seconds is roughly year 5138.
 * Current time (2025) is ~1.7e9 seconds or ~1.7e12 milliseconds.
 *
 * @param date - The input date (number, string, or Date)
 * @returns The timestamp in milliseconds, or null if invalid
 */
export const normalizeTimestamp = (
  date: string | number | Date | null | undefined
): number | null => {
  if (date === null || date === undefined) return null

  if (date instanceof Date) {
    const time = date.getTime()
    return isNaN(time) ? null : time
  }

  let numericValue: number

  if (typeof date === 'string') {
    // Try parsing as number first
    const parsed = Number(date)
    if (!isNaN(parsed)) {
      numericValue = parsed
    } else {
      // Try parsing as date string
      const parsedDate = Date.parse(date)
      return isNaN(parsedDate) ? null : parsedDate
    }
  } else {
    numericValue = date
  }

  if (isNaN(numericValue)) return null

  // Heuristic check
  // If less than 100,000,000,000 (100 billion), assume seconds
  if (Math.abs(numericValue) < 100000000000) {
    return numericValue * 1000
  }

  return numericValue
}
