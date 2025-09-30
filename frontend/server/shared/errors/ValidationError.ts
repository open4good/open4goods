import { DomainError } from './DomainError'

/**
 * Error thrown when input validation fails
 */
export class ValidationError extends DomainError {
  readonly code = 'VALIDATION_ERROR'
  readonly statusCode = 400

  constructor(
    message: string,
    public readonly fields?: Record<string, string[]>,
    cause?: unknown
  ) {
    super(message, cause)
  }

  override toJSON() {
    return {
      ...super.toJSON(),
      fields: this.fields,
    }
  }
}
