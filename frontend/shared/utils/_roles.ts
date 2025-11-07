export const hasAdminAccess = (roles?: readonly (string | null | undefined)[] | null) => {
  if (!roles || roles.length === 0) {
    return false
  }

  return roles.some((role) => {
    if (!role) {
      return false
    }

    const normalized = role.trim().toLowerCase()
    return normalized === 'admin' || normalized === 'role_admin'
  })
}
