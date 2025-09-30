export const useAuthCookies = () => {
  const config = useRuntimeConfig()
  const tokenCookieName = config.public.tokenCookieName ?? config.tokenCookieName
  const refreshCookieName = config.public.refreshCookieName ?? config.refreshCookieName

  const tokenCookie = useCookie<string | null>(tokenCookieName)
  const refreshCookie = useCookie<string | null>(refreshCookieName)

  return { tokenCookie, refreshCookie }
}
