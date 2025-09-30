/**
 * Simple Dependency Injection Container
 * Uses factory functions and singleton pattern
 */

type Factory<T> = () => T
type ServiceMap = Map<string, unknown>

class Container {
  private services: ServiceMap = new Map()
  private factories: Map<string, Factory<unknown>> = new Map()

  /**
   * Register a factory function for a service
   */
  register<T>(key: string, factory: Factory<T>): void {
    this.factories.set(key, factory)
  }

  /**
   * Get a service instance (singleton)
   * Creates it on first call, then caches it
   */
  get<T>(key: string): T {
    if (this.services.has(key)) {
      return this.services.get(key) as T
    }

    const factory = this.factories.get(key)
    if (!factory) {
      throw new Error(`Service '${key}' not registered in container`)
    }

    const instance = factory() as T
    this.services.set(key, instance)
    return instance
  }

  /**
   * Check if a service is registered
   */
  has(key: string): boolean {
    return this.factories.has(key)
  }

  /**
   * Clear all services (useful for testing)
   */
  clear(): void {
    this.services.clear()
  }

  /**
   * Reset the container (clear services and factories)
   */
  reset(): void {
    this.services.clear()
    this.factories.clear()
  }
}

// Export singleton instance
export const container = new Container()
