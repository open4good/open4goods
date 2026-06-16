export default defineNuxtRouteMiddleware(async (to) => {
  const { session, fetchMe } = useAuthSession()

  if (!session.value) {
    await fetchMe()
  }

  const isAdmin = session.value?.user?.platformAdmin === true

  if (!session.value || !isAdmin) {
    const next = encodeURIComponent(to.fullPath)
    return navigateTo(`/auth/login?next=${next}&error=insufficient_role&required=admin&current=user`)
  }
})
