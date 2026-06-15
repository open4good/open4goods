export default defineNuxtRouteMiddleware(async (to) => {
  const { session, fetchMe } = useAuthSession()

  if (!session.value) {
    await fetchMe()
  }

  if (!session.value) {
    const next = encodeURIComponent(to.fullPath)
    return navigateTo(`/auth/login?next=${next}`)
  }
})
