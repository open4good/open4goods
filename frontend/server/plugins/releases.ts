import { warmReleaseCache } from '../utils/releases'

export default defineNitroPlugin(async () => {
  await warmReleaseCache()
})
