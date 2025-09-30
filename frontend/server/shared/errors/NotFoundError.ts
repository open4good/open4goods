import { DomainError } from './DomainError'

/**
 * Error thrown when a resource is not found
 */
export class NotFoundError extends DomainError {
  readonly code = 'NOT_FOUND'
  readonly statusCode = 404

  constructor(resource: string, identifier: string, cause?: unknown) {
    super(`${resource} with identifier '${identifier}' not found`, cause)
    this.resource = resource
    this.identifier = identifier
  }

  public readonly resource: string
  public readonly identifier: string

  toJSON() {
    return {
      ...super.toJSON(),
      resource: this.resource,
      identifier: this.identifier,
    }
  }
}
