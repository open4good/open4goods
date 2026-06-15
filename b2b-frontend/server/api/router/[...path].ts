import { proxyRemoteApi } from '~/server/utils/remoteApiProxy'

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)

  return proxyRemoteApi(event, {
    target: config.routerProxyTarget,
    mountPath: '/api/router',
    allowMutatingMethods: config.remoteProxyAllowMutations
  })
})
