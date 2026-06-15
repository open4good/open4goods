import { proxyRemoteApi } from '~/server/utils/remoteApiProxy'

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)

  return proxyRemoteApi(event, {
    target: config.backendProxyTarget,
    mountPath: '/api/backend',
    allowMutatingMethods: config.remoteProxyAllowMutations,
    allowedMutationPathPrefixes: ['/api/v1/auth/', '/api/v1/customer/']
  })
})
