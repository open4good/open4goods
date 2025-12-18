export const useAuthCookies = () => {
  const config = useRuntimeConfig()
  const tokenCookie = useCookie<string | null>(config.public.tokenCookieName)
  const refreshCookie = useCookie<string | null>(
    config.public.refreshCookieName
  )

  return { tokenCookie, refreshCookie }
}
