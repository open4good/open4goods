import { DomainError } from './DomainError'

/**
 * Error thrown when infrastructure layer fails
 * (network issues, external API errors, etc.)
 */
export class InfrastructureError extends DomainError {
  readonly code = 'INFRASTRUCTURE_ERROR'
  readonly statusCode: number

  constructor(message: string, statusCode: number = 500, cause?: unknown) {
    super(message, cause)
    this.statusCode = statusCode
  }
}
