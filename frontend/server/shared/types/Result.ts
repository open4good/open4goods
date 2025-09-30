/**
 * Result pattern for functional error handling
 * Inspired by Rust's Result type
 */
export type Result<T, E = Error> = Success<T> | Failure<E>

export interface Success<T> {
  success: true
  value: T
}

export interface Failure<E> {
  success: false
  error: E
}

export const success = <T>(value: T): Success<T> => ({
  success: true,
  value,
})

export const failure = <E>(error: E): Failure<E> => ({
  success: false,
  error,
})

/**
 * Type guard to check if result is successful
 */
export const isSuccess = <T, E>(result: Result<T, E>): result is Success<T> => {
  return result.success
}

/**
 * Type guard to check if result is a failure
 */
export const isFailure = <T, E>(result: Result<T, E>): result is Failure<E> => {
  return !result.success
}

/**
 * Map the value of a successful result
 */
export const map = <T, U, E>(
  result: Result<T, E>,
  fn: (value: T) => U
): Result<U, E> => {
  if (isSuccess(result)) {
    return success(fn(result.value))
  }
  return result
}

/**
 * Map the error of a failed result
 */
export const mapError = <T, E, F>(
  result: Result<T, E>,
  fn: (error: E) => F
): Result<T, F> => {
  if (isFailure(result)) {
    return failure(fn(result.error))
  }
  return result as Result<T, F>
}

/**
 * Unwrap the value or throw the error
 */
export const unwrap = <T, E>(result: Result<T, E>): T => {
  if (isSuccess(result)) {
    return result.value
  }
  throw result.error
}

/**
 * Unwrap the value or return a default
 */
export const unwrapOr = <T, E>(result: Result<T, E>, defaultValue: T): T => {
  if (isSuccess(result)) {
    return result.value
  }
  return defaultValue
}
