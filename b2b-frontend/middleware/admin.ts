export default defineNuxtRouteMiddleware(async (to) => {
  const { session, fetchMe } = useAuthSession()

  if (!session.value) {
    await fetchMe()
  }

  const isAdmin = session.value?.level === 'admin' || session.value?.roles?.includes('ROLE_ADMIN')
  
  if (!session.value || !isAdmin) {
    const currentLevel = session.value?.level || 'public'
    const next = encodeURIComponent(to.fullPath)
    const required = encodeURIComponent('admin')
    const current = encodeURIComponent(currentLevel)

    console.warn('[auth/rbac] Admin route denied', {
      target: to.fullPath,
      required: 'admin',
      current: currentLevel,
      roles: session.value?.roles || []
    })

    return navigateTo(`/auth/login?next=${next}&error=insufficient_role&required=${required}&current=${current}`)
  }
})
