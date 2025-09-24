export const useAuthCookies = () => {
  const config = useRuntimeConfig()
  const tokenCookie = useCookie<string | null>(config.tokenCookieName)
  const refreshCookie = useCookie<string | null>(config.refreshCookieName)

  return { tokenCookie, refreshCookie }
}
