import { useRuntimeConfig } from '#imports'

const DEFAULT_EDIT_ROLES = ['ROLE_SITEEDITOR', 'XWIKIADMINGROUP']

const normalizeRoles = (roles: readonly (string | null | undefined)[]) =>
  roles
    .map(role => role?.trim().toLowerCase())
    .filter((role): role is string => Boolean(role))

const getNormalizedEditRoles = (
  override?: readonly (string | null | undefined)[] | null
) => {
  if (override && override.length > 0) {
    return normalizeRoles(override)
  }

  const config = useRuntimeConfig()
  const configuredRoles = config?.public?.editRoles

  const roles =
    Array.isArray(configuredRoles) && configuredRoles.length > 0
      ? configuredRoles
      : DEFAULT_EDIT_ROLES

  return normalizeRoles(roles)
}

type AdminAccessOptions = {
  allowedRoles?: readonly (string | null | undefined)[] | null
}

export const hasAdminAccess = (
  roles?: readonly (string | null | undefined)[] | null,
  options?: AdminAccessOptions
) => {
  if (!roles || roles.length === 0) {
    return false
  }

  const allowedRoles = getNormalizedEditRoles(options?.allowedRoles)

  return roles.some(role => {
    if (!role) {
      return false
    }

    const normalized = role.trim().toLowerCase()
    return allowedRoles.includes(normalized)
  })
}
